package io.pleo.antaeus.core.channel.inbound

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.cloud.pubsub.v1.Subscriber
import com.google.pubsub.v1.ProjectSubscriptionName
import io.grpc.ManagedChannelBuilder
import io.pleo.antaeus.core.services.BillingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging

class TriggerSubscriber(
  private val billingService: BillingService,
) {

  private val log = KotlinLogging.logger {}

  private val triggerHandler = MessageReceiver { message, consumer ->
    log.info { "Received message with id ${message.messageId}, data: ${message.data.toStringUtf8()}" }
    CoroutineScope(Dispatchers.Default).launch { billingService.performBilling() }
    consumer.ack()
  }

  fun subscribe(projectId: String = "pleo", subscriptionId: String = "antaeus_svc-billing_trigger") {
    val subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId)

    val emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST")

    val subscriber = if (emulatorHost.isNullOrBlank()) {
      Subscriber.newBuilder(subscriptionName, triggerHandler).build()
    } else {
      log.info { "Using pubsub emulator located at $emulatorHost" }
      val channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build()
      val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      Subscriber.newBuilder(subscriptionName, triggerHandler)
        .setChannelProvider(channelProvider)
        .setCredentialsProvider(NoCredentialsProvider.create())
        .build()
    }

    subscriber.startAsync().awaitRunning()
    log.info { "Listening for messages on $subscriptionName" }
  }

}