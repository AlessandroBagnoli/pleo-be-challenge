import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

// This will create all schemas and setup initial data
internal fun setupInitialData(dal: AntaeusDal) {
  val customers = (1..100).mapNotNull {
    dal.createCustomer(
      currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
    )
  }

  customers.forEach { customer ->
    (1..10).forEach {
      dal.createInvoice(
        amount = Money(
          value = BigDecimal(Random.nextDouble(10.0, 500.0)),
          currency = customer.currency
        ),
        customer = customer,
        status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
      )
    }
  }
}

// This is the mocked instance of the payment provider
internal fun getPaymentProvider(): PaymentProvider {
  return object : PaymentProvider {
    override fun charge(invoice: Invoice): Boolean {
      return Random.nextBoolean()
    }
  }
}

// This calculates the time in seconds between now and the next first day of the month
internal fun everyFirstDayOfTheMonth(clock: Clock): Long {
  // Get the current date and time
  val now = LocalDateTime.now(clock)

  // Get the first day of the next month
  val firstDayOfNextMonth = now.with(TemporalAdjusters.firstDayOfNextMonth())

  val offset = clock.zone.rules.getOffset(now)

  // Calculate the delay until the first day of the next month
  return firstDayOfNextMonth.toLocalDate().atStartOfDay().toEpochSecond(offset) - now.toEpochSecond(offset)
}