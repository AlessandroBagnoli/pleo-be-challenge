package io.pleo.antaeus.core.channel.outbound

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.Publisher
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import com.google.pubsub.v1.TopicName
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging

class NotificationPublisher {

  private val log = KotlinLogging.logger {}
  private val publisher = buildPublisher()

  fun publish(notification: String) {
    val data = ByteString.copyFromUtf8(notification)
    val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()

    val messageIdFuture = publisher.publish(pubsubMessage)

    // TODO handle the get?, it is synch
    val messageId = messageIdFuture.get()
    log.info { "Published message ID: $messageId" }
  }

  private fun buildPublisher(): Publisher {
    val topicName = TopicName.of("pleo", "notifications")

    val emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST")

    val publisher = if (emulatorHost.isNullOrBlank()) {
      Publisher.newBuilder(topicName).build()
    } else {
      log.info { "Using pubsub emulator located at $emulatorHost" }
      val channel = ManagedChannelBuilder.forTarget(emulatorHost).usePlaintext().build()
      val channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
      Publisher.newBuilder(topicName)
        .setChannelProvider(channelProvider)
        .setCredentialsProvider(NoCredentialsProvider.create())
        .build()
    }

    return publisher
  }

}