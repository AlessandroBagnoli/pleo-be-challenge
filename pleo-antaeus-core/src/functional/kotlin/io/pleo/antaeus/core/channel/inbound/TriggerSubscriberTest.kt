package io.pleo.antaeus.core.channel.inbound

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.PUBSUB_IMG
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.billingTriggerPublisher
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class TriggerSubscriberTest {

  companion object {

    private val billingService = mockk<BillingService>()

    private val underTest = TriggerSubscriber(billingService)

    @JvmStatic
    @Container
    private val pubsubEmulator = PubSubEmulatorContainer(
      DockerImageName.parse(PUBSUB_IMG).asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk")
    )

    @JvmStatic
    @BeforeAll
    internal fun beforeAll() {
      PubSubTestConfig.setupPubSubEmulator(pubsubEmulator)
      underTest.subscribe(
        projectId = PubSubTestConfig.PROJECT_ID,
        subscriptionId = PubSubTestConfig.BILLING_TRIGGER_SUB.subscription,
        host = pubsubEmulator.emulatorEndpoint
      )
    }

  }

  @Test
  fun `should receive trigger and call processPending`() {
    // given
    val publisher = billingTriggerPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint)
    justRun { billingService.processPending() }

    // when
    val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(InvoiceStatus.PENDING.name)).build()
    val publish = publisher.publish(pubsubMessage)
    val actual = publish.get()

    // then
    assertTrue(actual.isNotEmpty())
    verify(timeout = 1000) { billingService.processPending() }
  }

  @Test
  fun `should receive trigger and call processRetry`() {
    // given
    val publisher = billingTriggerPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint)
    justRun { billingService.processRetry() }

    // when
    val pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(InvoiceStatus.RETRY.name)).build()
    val publish = publisher.publish(pubsubMessage)
    val actual = publish.get()

    // then
    assertTrue(actual.isNotEmpty())
    verify(timeout = 1000) { billingService.processRetry() }
  }

}