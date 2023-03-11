package io.pleo.antaeus.core.channel.outbound

import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.cloud.pubsub.v1.Publisher
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.pleo.antaeus.core.buildPublisher
import io.pleo.antaeus.models.Notification
import mu.KotlinLogging

class NotificationPublisher(
  private val publisher: Publisher = buildPublisher("pleo", "notifications")
) {

  private val log = KotlinLogging.logger {}

  fun publish(notification: Notification) {
    val data = ByteString.copyFromUtf8(Gson().toJson(notification))
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

}