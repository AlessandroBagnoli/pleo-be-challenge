package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.channel.outbound.InvoicePublisher
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

class BillingService(
  private val invoiceService: InvoiceService,
  private val invoicePublisher: InvoicePublisher
) {

  private val log = KotlinLogging.logger {}

  fun processPending() = performForStatus(InvoiceStatus.PENDING)

  fun processRetry() = performForStatus(InvoiceStatus.RETRY)

  private fun performForStatus(status: InvoiceStatus) {
    CoroutineScope(Dispatchers.Default).launch {
      log.info { "Started billing process" }
      invoiceService.fetchByStatus(status)
        .also { log.info { "Found ${it.size} invoices in $status status to process" } }
        .forEach { invoicePublisher.publish(it) }
      log.info { "Ended billing process" }
    }
  }

}
