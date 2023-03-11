package io.pleo.antaeus.core.channel.inbound

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.gson.Gson
import io.pleo.antaeus.core.buildSubscriber
import io.pleo.antaeus.core.services.InvoiceHandler
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class InvoiceSubscriber(
  private val invoiceHandler: InvoiceHandler
) {

  private val log = KotlinLogging.logger {}

  private val messageHandler = MessageReceiver { message, consumer ->
    try {
      log.info { "Received message with id ${message.messageId}, data: ${message.data.toStringUtf8()}" }
      invoiceHandler.process(Gson().fromJson(message.data.toStringUtf8(), Invoice::class.java))
      consumer.ack()
    } catch (ex: Exception) {
      log.warn(ex) { "Exception in message receiver for message with id ${message.messageId}, data: ${message.data.toStringUtf8()}" }
      consumer.nack()
    }
  }

  fun subscribe(
    projectId: String = "pleo",
    subscriptionId: String = "antaeus_svc-invoices",
    host: String = System.getenv("PUBSUB_EMULATOR_HOST")
  ) {
    val subscriber =
      buildSubscriber(project = projectId, subscription = subscriptionId, host = host, handler = messageHandler)
    subscriber.startAsync().awaitRunning()
    log.info { "Listening for messages on ${subscriber.subscriptionNameString}" }
  }

}