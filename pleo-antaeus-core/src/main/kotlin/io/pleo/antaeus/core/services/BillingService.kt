package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class BillingService(
  private val paymentProvider: PaymentProvider,
  private val invoiceService: InvoiceService
) {

  private val log = KotlinLogging.logger {}

  // TODO setup an asynch notification mechanism, using eg pubsub?
  fun performBilling() {
    log.info { "Started billing process" }
    invoiceService.fetchByStatus(InvoiceStatus.PENDING)
      .also { log.info { "Found ${it.size} invoices in PENDING status to process" } }
      .forEach { invoice ->
        val charged = try {
          paymentProvider.charge(invoice)
        } catch (ex: Exception) {
          when (ex) {
            // TODO log everything at error level?
            is CurrencyMismatchException -> {
              log.error(ex) { "Currency and customer account mismatch during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
            }

            is CustomerNotFoundException -> {
              log.error(ex) { "Customer with id ${invoice.customerId} not found during charge of invoice ${invoice.id}" }
            }

            is NetworkException -> {
              log.error(ex) { "Network error happened during charge of invoice ${invoice.id} for customer ${invoice.customerId}" }
            }
          }

          false
        }

        // TODO introduce a new status for retry/unrecoverable error?
        if (charged) {
          invoiceService.updateStatus(invoice.id, InvoiceStatus.PAID).also {
            log.info { "Customer account ${invoice.customerId} charged the given amount for invoice ${invoice.id}" }
          }
          return
        }

        log.info { "Customer ${invoice.customerId} charged for invoice ${invoice.id}" }

      }
  }

}
