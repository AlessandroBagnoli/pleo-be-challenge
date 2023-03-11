package io.pleo.antaeus.core.channel.inbound

import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.invoicePublisher
import io.pleo.antaeus.core.services.InvoiceHandler
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

@Testcontainers
class InvoiceSubscriberTest {

  companion object {

    private val invoiceHandler = mockk<InvoiceHandler>()

    private val underTest = InvoiceSubscriber(invoiceHandler)

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
      underTest.subscribe(
        projectId = PubSubTestConfig.PROJECT_ID,
        subscriptionId = PubSubTestConfig.INVOICES_SUB.subscription,
        host = pubsubEmulator.emulatorEndpoint
      )
    }

  }

  @Test
  fun `should receive trigger and call processPending`() {
    // given
    val publisher = invoicePublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint)
    val invoice = Invoice(
      id = 1,
      customerId = 23,
      amount = Money(BigDecimal("120.50"), Currency.EUR),
      status = InvoiceStatus.PENDING
    )
    justRun { invoiceHandler.process(invoice) }
    val data = ByteString.copyFromUtf8(Gson().toJson(invoice))
    val pubsubMessage = PubsubMessage.newBuilder().setData(data).build()

    // when
    val publish = publisher.publish(pubsubMessage)
    val actual = publish.get()

    // then
    assertTrue(actual.isNotEmpty())
    verify(timeout = 1000) { invoiceHandler.process(invoice) }
  }

}