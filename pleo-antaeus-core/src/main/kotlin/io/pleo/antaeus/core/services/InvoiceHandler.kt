package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class InvoiceHandler(
  private val paymentProvider: PaymentProvider,
  private val invoiceService: InvoiceService,
  private val notificationPublisher: NotificationPublisher
) {

  private val log = KotlinLogging.logger {}

  fun process(invoice: Invoice) {
    try {
      val charged = paymentProvider.charge(invoice)
      if (charged) {
        invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID)
        log.info { "Customer account ${invoice.customerId} charged the given amount for invoice ${invoice.id}" }
        notificationPublisher.publish("some cool notification :)")
        return
      }

      invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
      log.info { "Customer account ${invoice.customerId} balance did not allow the charge for invoice ${invoice.id}" }
    } catch (ex: Exception) {
      when (ex) {
        is CurrencyMismatchException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED)
          log.warn(ex) { "Currency and customer account mismatch during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
        }

        is CustomerNotFoundException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED)
          log.warn(ex) { "Customer with id ${invoice.customerId} not found during charge of invoice ${invoice.id}" }
        }

        is NetworkException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
          log.warn(ex) { "Network error happened during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
        }

        else -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
          log.warn(ex) { "Unknown exception during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
        }
      }
    }
  }
}