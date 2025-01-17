/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import getPaymentProvider
import io.pleo.antaeus.core.channel.inbound.InvoiceSubscriber
import io.pleo.antaeus.core.channel.inbound.TriggerSubscriber
import io.pleo.antaeus.core.channel.outbound.InvoicePublisher
import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceHandler
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData

fun main() {
  // The tables to create in the database.
  val tables = arrayOf(InvoiceTable, CustomerTable)

  // Connection pool configuration
  val config = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://${System.getenv("POSTGRES_HOST")}/antaeus"
    driverClassName = "org.postgresql.Driver"
    username = "pleo"
    password = "randompassword"
    maximumPoolSize = 10
    transactionIsolation = "TRANSACTION_SERIALIZABLE"
  }
  val dataSource = HikariDataSource(config)
  // Connect to the database and create the needed tables. Drop any existing data.
  val db = Database.connect(dataSource).also {
    transaction(it) {
      addLogger(StdOutSqlLogger)
      // Drop all existing tables to ensure a clean slate on each run
      SchemaUtils.drop(*tables)
      // Create all tables
      SchemaUtils.create(*tables)
    }
  }

  // Set up data access layer.
  val dal = AntaeusDal(db = db)

  // Insert example data in the database.
  setupInitialData(dal = dal)

  // Get third parties
  val paymentProvider = getPaymentProvider()

  // Create core services
  val invoiceService = InvoiceService(dal = dal)
  val customerService = CustomerService(dal = dal)
  val notificationPublisher = NotificationPublisher()
  val invoicePublisher = InvoicePublisher()
  val invoiceHandler = InvoiceHandler(
    paymentProvider = paymentProvider,
    invoiceService = invoiceService,
    notifier = notificationPublisher
  )

  // This is _your_ billing service to be included where you see fit
  val billingService = BillingService(invoiceService = invoiceService, invoicePublisher = invoicePublisher)

  // Create subscriber for trigger event and start listening
  val triggerSubscriber = TriggerSubscriber(billingService = billingService)
  triggerSubscriber.subscribe()

  // Create subscriber which receives invoices one by one and starts listening
  val invoiceSubscriber = InvoiceSubscriber(invoiceHandler = invoiceHandler)
  invoiceSubscriber.subscribe()

  // Create REST web service
  AntaeusRest(
    invoiceService = invoiceService,
    customerService = customerService
  ).run()
}
