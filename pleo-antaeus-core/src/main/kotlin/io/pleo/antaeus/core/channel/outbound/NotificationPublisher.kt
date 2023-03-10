package io.pleo.antaeus.core.channel.outbound

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.Publisher
import com.google.common.util.concurrent.MoreExecutors
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

    ApiFutures.addCallback(messageIdFuture, object : ApiFutureCallback<String> {
      override fun onSuccess(messageId: String) {
        log.info { "Published notification with message id: $messageId" }
      }

      override fun onFailure(t: Throwable) {
        log.warn(t) { "Error during publish of notification" }
      }
    }, MoreExecutors.directExecutor())
  }

  private fun buildPublisher(): Publisher {
    val topicName = TopicName.of("pleo", "notifications")

    // TODO to fix
    val emulatorHost = System.getProperty("PUBSUB_EMULATOR_HOST")

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