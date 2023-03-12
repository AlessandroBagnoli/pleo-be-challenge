package io.pleo.antaeus.core.flow

import com.google.cloud.pubsub.v1.MessageReceiver
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.pubsub.v1.PubsubMessage
import io.pleo.antaeus.core.channel.inbound.InvoiceSubscriber
import io.pleo.antaeus.core.channel.inbound.TriggerSubscriber
import io.pleo.antaeus.core.channel.outbound.InvoicePublisher
import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.config.PubSubTestConfig
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceHandler
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.*
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.awaitility.kotlin.untilAsserted
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PubSubEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.sql.Connection

@Testcontainers
class BillingFlowTest {

  companion object {

    private val tables = arrayOf(InvoiceTable, CustomerTable)

    private lateinit var db: Database
    private lateinit var dal: AntaeusDal
    private lateinit var underTest: TriggerSubscriber

    @JvmStatic
    @Container
    private val postgresEmulator = PostgreSQLContainer(DockerImageName.parse("postgres:15.2"))
      .withDatabaseName("antaeus")
      .withUsername("pleo")
      .withPassword("randompassword")

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

      db = Database
        .connect(
          url = postgresEmulator.jdbcUrl,
          driver = "org.postgresql.Driver",
          user = postgresEmulator.username,
          password = postgresEmulator.password
        )
        .also {
          TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        }

      // dummy implementation which returns always true
      val paymentProvider = object : PaymentProvider {
        override fun charge(invoice: Invoice) = true
      }

      dal = AntaeusDal(db = db)
      val invoiceService = InvoiceService(dal = dal)
      val notificationPublisher =
        NotificationPublisher(PubSubTestConfig.notificationPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint))
      val invoicePublisher =
        InvoicePublisher(PubSubTestConfig.invoicePublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint))
      val invoiceHandler = InvoiceHandler(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        notifier = notificationPublisher
      )
      val billingService = BillingService(invoiceService = invoiceService, invoicePublisher = invoicePublisher)

      val invoiceSubscriber = InvoiceSubscriber(invoiceHandler = invoiceHandler)
      invoiceSubscriber.subscribe(
        projectId = PubSubTestConfig.PROJECT_ID,
        subscriptionId = PubSubTestConfig.INVOICES_SUB.subscription,
        host = pubsubEmulator.emulatorEndpoint
      )

      underTest = TriggerSubscriber(billingService = billingService)
      underTest.subscribe(
        projectId = PubSubTestConfig.PROJECT_ID,
        subscriptionId = PubSubTestConfig.BILLING_TRIGGER_SUB.subscription,
        host = pubsubEmulator.emulatorEndpoint
      )
    }

  }

  @BeforeEach
  fun setUp() {
    transaction(db) {
      addLogger(StdOutSqlLogger)
      SchemaUtils.drop(*tables)
      SchemaUtils.create(*tables)
    }
  }

  @Test
  fun `billing processing should send a notification for a successfully PAID invoice`() {
    // given
    val customer = dal.createCustomer(Currency.EUR)
    val invoice =
      dal.createInvoice(amount = Money(value = BigDecimal.TEN, currency = Currency.EUR), customer = customer!!)
    await until { dal.fetchInvoice(invoice!!.id)!!.status == InvoiceStatus.PENDING }
    val triggerPublisher = PubSubTestConfig.billingTriggerPublisher(emulatorEndpoint = pubsubEmulator.emulatorEndpoint)
    val trigger = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(InvoiceStatus.PENDING.name)).build()

    var receivedMessage = PubsubMessage.getDefaultInstance()
    val receiver = MessageReceiver { message, consumer ->
      receivedMessage = message
      consumer.ack()
    }
    PubSubTestConfig.notificationSubscriber(emulatorEndpoint = pubsubEmulator.emulatorEndpoint, receiver).startAsync()
      .awaitRunning()

    // when
    triggerPublisher.publish(trigger)

    // then
    val expectedNotification =
      Notification(customerId = customer.id, invoiceId = invoice!!.id, text = "Your invoice has been paid!")
    await untilAsserted {
      assertEquals(expectedNotification, Gson().fromJson(receivedMessage.data.toStringUtf8(), Notification::class.java))
    }
    assertEquals(InvoiceStatus.PAID, dal.fetchInvoice(invoice.id)!!.status)
  }

}