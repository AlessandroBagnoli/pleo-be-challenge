package io.pleo.antaeus.data

import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.math.BigDecimal
import java.sql.Connection
import kotlin.random.Random

class AntaeusDalTest {

  companion object {

    private lateinit var db: Database

    private val tables = arrayOf(InvoiceTable, CustomerTable)

    @JvmStatic
    @BeforeAll
    fun beforeAll() {
      val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
      // Connect to the database and create the needed tables. Drop any existing data.
      db = Database
        .connect(
          url = "jdbc:sqlite:${dbFile.absolutePath}",
          driver = "org.sqlite.JDBC",
          user = "root",
          password = ""
        )
        .also {
          TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
        }
    }

  }

  private lateinit var underTest: AntaeusDal

  @BeforeEach
  fun setUp() {
    transaction(db) {
      addLogger(StdOutSqlLogger)
      // Drop all existing tables to ensure a clean slate on each run
      SchemaUtils.drop(*tables)
      // Create all tables
      SchemaUtils.create(*tables)
    }
    underTest = AntaeusDal(db)
  }

  @Nested
  @DisplayName("fetchInvoiceByStatus")
  inner class FetchInvoiceByStatus {

    @Test
    fun `should return invoices in desired status`() {
      // given
      val customer = underTest.createCustomer(currency = Currency.EUR)
      val invoice = underTest.createInvoice(
        amount = Money(
          value = BigDecimal(Random.nextDouble(10.0, 500.0)),
          currency = customer!!.currency
        ),
        customer = customer,
        status = InvoiceStatus.PENDING
      )
      underTest.createInvoice(
        amount = Money(
          value = BigDecimal(Random.nextDouble(10.0, 500.0)),
          currency = customer.currency
        ),
        customer = customer,
        status = InvoiceStatus.PAID
      )

      // when
      val actual = underTest.fetchInvoiceByStatus(InvoiceStatus.PENDING)

      // then
      assertEquals(1, actual.size)
      assertEquals(invoice, actual[0])
    }

  }

  @Nested
  @DisplayName("updateInvoiceStatus")
  inner class UpdateInvoiceStatus {

    @Test
    fun `should update status correctly`() {
      // given
      val customer = underTest.createCustomer(currency = Currency.EUR)
      val invoice = underTest.createInvoice(
        amount = Money(
          value = BigDecimal(Random.nextDouble(10.0, 500.0)),
          currency = customer!!.currency
        ),
        customer = customer,
        status = InvoiceStatus.PENDING
      )

      // when
      val actual = underTest.updateInvoiceStatus(invoice!!.id, InvoiceStatus.PAID)

      // then
      assertEquals(1, actual)
      val postUpdate = underTest.fetchInvoice(invoice.id)
      assertEquals(InvoiceStatus.PAID, postUpdate!!.status)
    }

  }

}