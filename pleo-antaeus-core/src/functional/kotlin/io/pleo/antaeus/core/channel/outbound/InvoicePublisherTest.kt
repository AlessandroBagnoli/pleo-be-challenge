package io.pleo.antaeus.core.channel.outbound

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.gson.Gson
import com.google.pubsub.v1.PubsubMessage
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.invoicePublisher
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.invoiceSubscriber
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

@Testcontainers
class InvoicePublisherTest {

  companion object {

    private lateinit var underTest: InvoicePublisher

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
      underTest = InvoicePublisher(invoicePublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint))
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
    invoiceSubscriber(emulatorEndpoint = pubsubEmulator.emulatorEndpoint, receiver = receiver)
      .startAsync()
      .awaitRunning()
    val input = Invoice(
      id = 5,
      customerId = 10,
      amount = Money(value = BigDecimal.ONE, currency = Currency.EUR),
      status = InvoiceStatus.PENDING
    )

    // when
    underTest.publish(input)

    // then
    await untilAsserted {
      assertEquals(
        input,
        Gson().fromJson(receivedMessage.data.toStringUtf8(), Invoice::class.java)
      )
    }
  }

}