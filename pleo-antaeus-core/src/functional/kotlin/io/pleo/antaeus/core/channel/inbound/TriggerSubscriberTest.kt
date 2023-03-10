package io.pleo.antaeus.core.channel.inbound

import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.PUBSUB_IMG
import io.pleo.antaeus.core.config.PubSubTestConfig.Companion.billingTriggerPublisher
import io.pleo.antaeus.core.services.BillingService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@Testcontainers
class TriggerSubscriberTest {

  private lateinit var billingService: BillingService
  private lateinit var underTest: TriggerSubscriber

  companion object {

    @JvmStatic
    @Container
    private val pubsubEmulator = PubSubEmulatorContainer(
      DockerImageName.parse(PUBSUB_IMG).asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk")
    )

    @JvmStatic
    @BeforeAll
    internal fun beforeAll() {
      PubSubTestConfig.setupPubSubEmulator(pubsubEmulator)
      System.setProperty("PUBSUB_EMULATOR_HOST", pubsubEmulator.emulatorEndpoint)
    }

  }

  @BeforeEach
  fun setUp() {
    billingService = mockk()
    underTest = TriggerSubscriber(billingService)
  }

  @Test
  fun subscribe() {
    // given
    val publisher = billingTriggerPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint)
    coJustRun { billingService.performBilling() }

    // when
    underTest.subscribe(
      projectId = PubSubTestConfig.PROJECT_ID,
      subscriptionId = PubSubTestConfig.BILLING_TRIGGER_SUB.subscription
    )
    val publish = publisher.publish(PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("something")).build())
    val actual = publish.get()

    // then
    assertTrue(actual.isNotEmpty())
    coVerify(timeout = 5000) { billingService.performBilling() }
  }
}