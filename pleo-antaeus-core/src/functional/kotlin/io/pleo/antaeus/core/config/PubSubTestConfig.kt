package io.pleo.antaeus.core.config

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.api.gax.rpc.TransportChannelProvider
import com.google.cloud.pubsub.v1.*
import com.google.pubsub.v1.ProjectSubscriptionName
import com.google.pubsub.v1.PushConfig
import com.google.pubsub.v1.SubscriptionName
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import org.testcontainers.containers.PubSubEmulatorContainer

internal class PubSubTestConfig {

  companion object {

    const val PUBSUB_IMG = "gcr.io/google.com/cloudsdktool/google-cloud-cli:417.0.0-emulators"
    const val PROJECT_ID = "pleo"

    private val BILLING_TRIGGER_TOPIC = TopicName.of(PROJECT_ID, "billing_trigger")
    val NOTIFICATIONS_TOPIC: TopicName = TopicName.of(PROJECT_ID, "notifications")
    val BILLING_TRIGGER_SUB: SubscriptionName = SubscriptionName.of(PROJECT_ID, "antaeus_svc-billing_trigger")
    private val NOTIFICATIONS_SUB = SubscriptionName.of(PROJECT_ID, "antaeus_svc-notifications")

    private val pubsubList = mapOf(
      BILLING_TRIGGER_TOPIC to BILLING_TRIGGER_SUB,
      NOTIFICATIONS_TOPIC to NOTIFICATIONS_SUB
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

    fun billingTriggerPublisher(emulatorEndpoint: String): Publisher {
      val channel = ManagedChannelBuilder.forTarget(emulatorEndpoint).usePlaintext().build()
      val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      return Publisher.newBuilder(BILLING_TRIGGER_TOPIC)
        .setChannelProvider(channelProvider)
        .setCredentialsProvider(NoCredentialsProvider.create())
        .build()
    }

    fun notificationSubscriber(emulatorEndpoint: String, receiver: MessageReceiver): Subscriber {
      val channel = ManagedChannelBuilder.forTarget(emulatorEndpoint).usePlaintext().build()
      val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      return Subscriber.newBuilder(
        ProjectSubscriptionName.of(
          NOTIFICATIONS_SUB.project,
          NOTIFICATIONS_SUB.subscription
        ), receiver
      )
        .setChannelProvider(channelProvider)
        .setCredentialsProvider(NoCredentialsProvider.create())
        .build()
    }

  }
}