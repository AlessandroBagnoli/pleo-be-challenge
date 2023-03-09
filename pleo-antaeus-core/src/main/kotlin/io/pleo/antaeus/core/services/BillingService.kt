package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import mu.KotlinLogging

class BillingService(
  private val paymentProvider: PaymentProvider,
  private val invoiceService: InvoiceService
) {

  private val log = KotlinLogging.logger {}

  suspend fun performBilling() = coroutineScope {
    log.info { "Started billing process" }
    invoiceService.fetchByStatus(InvoiceStatus.PENDING)
      .also { log.info { "Found ${it.size} invoices in PENDING status to process" } }
      .map { launch { process(it) } }
      .joinAll()
    log.info { "Ended billing process" }
  }

  private fun process(invoice: Invoice) {
    try {
      val charged = paymentProvider.charge(invoice)
      if (charged) {
        invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID).also {
          log.info { "Customer account ${invoice.customerId} charged the given amount for invoice ${invoice.id}" }
        }
        return
      }

      invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
        .also { log.info { "Customer account ${invoice.customerId} balance did not allow the charge for invoice ${invoice.id}" } }
    } catch (ex: Exception) {
      when (ex) {
        is CurrencyMismatchException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED)
            .also { log.error(ex) { "Currency and customer account mismatch during charge of invoice ${invoice.id} for customer ${invoice.customerId}" } }
        }

        is CustomerNotFoundException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.FAILED)
            .also { log.error(ex) { "Customer with id ${invoice.customerId} not found during charge of invoice ${invoice.id}" } }
        }

        is NetworkException -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
            .also { log.warn(ex) { "Network error happened during charge of invoice ${invoice.id} for customer ${invoice.customerId}" } }
        }

        else -> {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.RETRY)
            .also { log.error(ex) { "Unknown exception during charge of invoice ${invoice.id} for customer ${invoice.customerId}" } }
        }
      }
    }
  }


}
