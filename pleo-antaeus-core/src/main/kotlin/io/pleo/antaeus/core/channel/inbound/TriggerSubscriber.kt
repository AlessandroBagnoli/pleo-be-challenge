package io.pleo.antaeus.core.channel.inbound

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.gson.Gson
import io.pleo.antaeus.core.buildSubscriber
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging

class TriggerSubscriber(
  private val billingService: BillingService,
) {

  private val log = KotlinLogging.logger {}

  private val triggerHandler = MessageReceiver { message, consumer ->
    try {
      log.info { "Received message with id ${message.messageId}, data: ${message.data.toStringUtf8()}" }
      when (val invoiceStatus = Gson().fromJson(message.data.toStringUtf8(), InvoiceStatus::class.java)) {
        InvoiceStatus.PENDING -> billingService.processPending()
        InvoiceStatus.RETRY -> billingService.processRetry()
        else -> log.info { "Unable to process invoices in status: $invoiceStatus" }
      }
    } catch (ex: Exception) {
      log.warn(ex) { "Exception in message receiver for message with id ${message.messageId}, data: ${message.data.toStringUtf8()}" }
    } finally {
      consumer.ack()
    }
  }

  fun subscribe(
    projectId: String = "pleo",
    subscriptionId: String = "antaeus_svc-billing_trigger",
    host: String = System.getenv("PUBSUB_EMULATOR_HOST")
  ) {
    val subscriber =
      buildSubscriber(project = projectId, subscription = subscriptionId, host = host, handler = triggerHandler)
    subscriber.startAsync().awaitRunning()
    log.info { "Listening for messages on ${subscriber.subscriptionNameString}" }
  }

}