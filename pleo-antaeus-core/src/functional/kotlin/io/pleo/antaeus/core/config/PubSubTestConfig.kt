package io.pleo.antaeus.core.config

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import io.pleo.antaeus.core.buildPublisher
import io.pleo.antaeus.core.buildSubscriber
import org.testcontainers.containers.PubSubEmulatorContainer

internal class PubSubTestConfig {

  companion object {

    const val PUBSUB_IMG = "gcr.io/google.com/cloudsdktool/google-cloud-cli:417.0.0-emulators"
    const val PROJECT_ID = "pleo"

    private val BILLING_TRIGGER_TOPIC = TopicName.of(PROJECT_ID, "billing_trigger")
    private val NOTIFICATIONS_TOPIC: TopicName = TopicName.of(PROJECT_ID, "notifications")
    private val INVOICES_TOPIC: TopicName = TopicName.of(PROJECT_ID, "invoices")
    val BILLING_TRIGGER_SUB: SubscriptionName = SubscriptionName.of(PROJECT_ID, "antaeus_svc-billing_trigger")
    private val NOTIFICATIONS_SUB = SubscriptionName.of(PROJECT_ID, "antaeus_svc-notifications")
    val INVOICES_SUB = SubscriptionName.of(PROJECT_ID, "antaeus_svc-invoices")

    private val pubsubList = mapOf(
      BILLING_TRIGGER_TOPIC to BILLING_TRIGGER_SUB,
      NOTIFICATIONS_TOPIC to NOTIFICATIONS_SUB,
      INVOICES_TOPIC to INVOICES_SUB
    )

    fun setupPubSubEmulator(pubsubEmulator: PubSubEmulatorContainer) {

      val channel =
        ManagedChannelBuilder.forTarget("dns:///" + pubsubEmulator.emulatorEndpoint)
          .usePlaintext()
          .build()
      val channelProvider: TransportChannelProvider =
        FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      val topicAdminClient =
        TopicAdminClient.create(
          TopicAdminSettings.newBuilder()
            .setCredentialsProvider(NoCredentialsProvider.create())
            .setTransportChannelProvider(channelProvider)
            .build()
        )
      val subscriptionAdminClient =
        SubscriptionAdminClient.create(
          SubscriptionAdminSettings.newBuilder()
            .setTransportChannelProvider(channelProvider)
            .setCredentialsProvider(NoCredentialsProvider.create())
            .build()
        )

      pubsubList.forEach { (topic, sub) ->
        topicAdminClient.createTopic(topic)
        subscriptionAdminClient.createSubscription(sub, topic, PushConfig.getDefaultInstance(), 10)
      }

      topicAdminClient.close()
      subscriptionAdminClient.close()
      channel.shutdown()
    }

    fun billingTriggerPublisher(emulatorEndpoint: String) =
      buildPublisher(project = PROJECT_ID, topic = BILLING_TRIGGER_TOPIC.topic, host = emulatorEndpoint)

    fun notificationPublisher(emulatorEndpoint: String) = buildPublisher(
      project = PROJECT_ID,
      topic = NOTIFICATIONS_TOPIC.topic,
      host = emulatorEndpoint
    )

    fun invoicePublisher(emulatorEndpoint: String) = buildPublisher(
      project = PROJECT_ID,
      topic = INVOICES_TOPIC.topic,
      host = emulatorEndpoint
    )

    fun notificationSubscriber(emulatorEndpoint: String, receiver: MessageReceiver) =
      buildSubscriber(
        project = PROJECT_ID,
        subscription = NOTIFICATIONS_SUB.subscription,
        host = emulatorEndpoint,
        handler = receiver
      )

    fun invoiceSubscriber(emulatorEndpoint: String, receiver: MessageReceiver) =
      buildSubscriber(
        project = PROJECT_ID,
        subscription = INVOICES_SUB.subscription,
        host = emulatorEndpoint,
        handler = receiver
      )

  }
}