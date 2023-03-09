package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.mockk.verifyAll
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
@OptIn(ExperimentalCoroutinesApi::class)
class BillingServiceTest {

  @MockK
  private lateinit var paymentProvider: PaymentProvider

  @MockK
  private lateinit var invoiceService: InvoiceService

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
    fun `should do nothing when no invoices in pending status`() = runTest {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns emptyList()

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verify { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
    }

    @Test
    fun `should set status to PAID when provider charges correctly`() = runTest {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } returns true
      every { invoiceService.updateStatus(1, InvoiceStatus.PAID) } returns 1

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verifyAll {
        invoiceService.fetchByStatus(InvoiceStatus.PENDING)
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.PAID)
      }
    }

    @Test
    fun `should set status to RETRY when provider does not charge`() = runTest {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } returns false
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verifyAll {
        invoiceService.fetchByStatus(InvoiceStatus.PENDING)
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.RETRY)
      }
    }

    @ParameterizedTest
    @MethodSource("io.pleo.antaeus.core.services.BillingServiceTest#exceptions for FAILED")
    fun `should set status to FAILED when provider throws CurrencyMismatchException or CustomerNotFoundException`(
      exception: Exception
    ) = runTest {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } throws exception
      every { invoiceService.updateStatus(1, InvoiceStatus.FAILED) } returns 1

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verifyAll {
        invoiceService.fetchByStatus(InvoiceStatus.PENDING)
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.FAILED)
      }
    }

    @ParameterizedTest
    @MethodSource("io.pleo.antaeus.core.services.BillingServiceTest#exceptions for RETRY")
    fun `should set status to RETRY when provider throws NetworkException or any other generic exception`(
      exception: Exception
    ) = runTest {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns listOf(dummyInvoice)
      every { paymentProvider.charge(dummyInvoice) } throws exception
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verifyAll {
        invoiceService.fetchByStatus(InvoiceStatus.PENDING)
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.RETRY)
      }
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