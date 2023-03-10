package io.pleo.antaeus.core.channel.outbound

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.pubsub.v1.PubsubMessage
import io.pleo.antaeus.core.config.PubSubTestConfig
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

  private val underTest = NotificationPublisher()

  companion object {

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
      System.setProperty("PUBSUB_EMULATOR_HOST", pubsubEmulator.emulatorEndpoint)
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
    PubSubTestConfig.notificationSubscriber(emulatorEndpoint = pubsubEmulator.emulatorEndpoint, receiver)
      .startAsync()
      .awaitRunning()

    // when
    underTest.publish("a cool notification")

    // then
    await untilAsserted { assertEquals("a cool notification", receivedMessage.data.toStringUtf8()) }
  }
}