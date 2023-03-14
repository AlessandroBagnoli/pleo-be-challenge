package io.pleo.antaeus.core.channel.outbound

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.gson.Gson
import com.google.pubsub.v1.PubsubMessage
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.notificationPublisher
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.notificationSubscriber
import io.pleo.antaeus.models.Notification
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class NotificationPublisherTest {

  companion object {

    private lateinit var underTest: NotificationPublisher

    @JvmStatic
    @Container
    private val pubsubEmulator = PubSubEmulatorContainer(
      DockerImageName.parse(PubSubTestConfig.PUBSUB_IMG)
        .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk")
    )

    @JvmStatic
    @BeforeAll
    internal fun beforeAll() {
      PubSubTestConfig.setupPubSubEmulator(pubsubEmulator)
      underTest = NotificationPublisher(notificationPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint))
    }

  }

  @Test
  fun `should publish correctly`() {
    // given
    var receivedMessage = PubsubMessage.getDefaultInstance()
    val receiver = MessageReceiver { message, consumer ->
      receivedMessage = message
      consumer.ack()
    }
    val input = Notification(customerId = 1, invoiceId = 2, text = "some random text")
    notificationSubscriber(emulatorEndpoint = pubsubEmulator.emulatorEndpoint, receiver)
      .startAsync()
      .awaitRunning()

    // when
    underTest.publish(input)

    // then
    await untilAsserted {
      assertEquals(
        input,
        Gson().fromJson(receivedMessage.data.toStringUtf8(), Notification::class.java)
      )
    }
  }
}