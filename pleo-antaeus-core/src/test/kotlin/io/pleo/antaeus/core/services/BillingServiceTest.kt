package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
@MockKExtension.ConfirmVerification
@MockKExtension.CheckUnnecessaryStub
class BillingServiceTest {

  @MockK
  private lateinit var paymentProvider: PaymentProvider

  @MockK
  private lateinit var invoiceService: InvoiceService

  @MockK
  private lateinit var notificationPublisher: NotificationPublisher

  @InjectMockKs
  private lateinit var underTest: BillingService

  private val dummyInvoice = Invoice(
    id = 1,
    customerId = 1,
    amount = Money(BigDecimal("120.50"), Currency.EUR),
    status = InvoiceStatus.PENDING
  )

  @Nested
  @DisplayName("performBilling")
  inner class PerformBilling {

    @Test
    fun `should do nothing when no invoices in pending status`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns emptyList()

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
    }

    @Test
    fun `should set status to PAID and send notification when provider charges correctly`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } returns true
      every { invoiceService.updateStatus(1, InvoiceStatus.PAID) } returns 1
      justRun { notificationPublisher.publish("some cool notification :)") }

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { paymentProvider.charge(dummyInvoice) }
      verify(timeout = 1000) { invoiceService.updateStatus(1, InvoiceStatus.PAID) }
      verify(timeout = 1000) { notificationPublisher.publish("some cool notification :)") }
    }

    @Test
    fun `should set status to RETRY when provider does not charge`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } returns false
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { paymentProvider.charge(dummyInvoice) }
      verify(timeout = 1000) { invoiceService.updateStatus(1, InvoiceStatus.RETRY) }
    }

    @ParameterizedTest
    @MethodSource("io.pleo.antaeus.core.services.BillingServiceTest#exceptions for FAILED")
    fun `should set status to FAILED when provider throws CurrencyMismatchException or CustomerNotFoundException`(
      exception: Exception
    ) {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } throws exception
      every { invoiceService.updateStatus(1, InvoiceStatus.FAILED) } returns 1

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { paymentProvider.charge(dummyInvoice) }
      verify(timeout = 1000) { invoiceService.updateStatus(1, InvoiceStatus.FAILED) }
    }

    @ParameterizedTest
    @MethodSource("io.pleo.antaeus.core.services.BillingServiceTest#exceptions for RETRY")
    fun `should set status to RETRY when provider throws NetworkException or any other generic exception`(
      exception: Exception
    ) {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } throws exception
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { paymentProvider.charge(dummyInvoice) }
      verify(timeout = 1000) { invoiceService.updateStatus(1, InvoiceStatus.RETRY) }
    }

  }

  companion object {
    @JvmStatic
    fun `exceptions for FAILED`() = listOf(
      Arguments.of(CurrencyMismatchException(invoiceId = 1, customerId = 1)),
      Arguments.of(CustomerNotFoundException(id = 1))
    )

    @JvmStatic
    fun `exceptions for RETRY`() = listOf(
      Arguments.of(NetworkException()),
      Arguments.of(RuntimeException("some random exception"))
    )

  }


}