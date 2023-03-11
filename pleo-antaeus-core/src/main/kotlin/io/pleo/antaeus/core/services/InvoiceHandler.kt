package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Notification
import mu.KotlinLogging

class InvoiceHandler(
  private val paymentProvider: PaymentProvider,
  private val invoiceService: InvoiceService,
  private val notifier: NotificationPublisher
) {

  companion object {
    private val log = KotlinLogging.logger {}
    private const val PAID_TEXT = "Your invoice has been paid!"
    private const val FAILED_TEXT = "An error has occurred during the payment of your invoice"
  }

  fun process(invoice: Invoice) {
    var result = invoice.status
    try {
      val charged = paymentProvider.charge(invoice)
      result = if (charged) {
        log.info { "Customer account ${invoice.customerId} charged the given amount for invoice ${invoice.id}" }
        notifier.publish(Notification(invoiceId = invoice.id, customerId = invoice.customerId, text = PAID_TEXT))
        InvoiceStatus.PAID
      } else {
        log.info { "Customer account ${invoice.customerId} balance did not allow the charge for invoice ${invoice.id}" }
        InvoiceStatus.RETRY
      }
    } catch (ex: Exception) {
      result = when (ex) {
        is CurrencyMismatchException,
        is CustomerNotFoundException -> {
          log.warn(ex) { "Unrecoverable exception during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
          notifier.publish(Notification(invoiceId = invoice.id, customerId = invoice.customerId, text = FAILED_TEXT))
          InvoiceStatus.FAILED
        }

        is NetworkException -> {
          log.warn(ex) { "Network error happened during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
          InvoiceStatus.RETRY
        }

        else -> {
          log.warn(ex) { "Unknown exception during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
          notifier.publish(Notification(invoiceId = invoice.id, customerId = invoice.customerId, text = FAILED_TEXT))
          InvoiceStatus.FAILED
        }
      }
    } finally {
      invoiceService.updateStatus(invoice.id, result)
      log.info { "Updated invoice ${invoice.id} to status $result" }
    }
  }
}