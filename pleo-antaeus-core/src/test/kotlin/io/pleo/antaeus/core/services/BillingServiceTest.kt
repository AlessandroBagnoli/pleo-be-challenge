package io.pleo.antaeus.core.services

import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verify
import io.pleo.antaeus.core.channel.outbound.InvoicePublisher
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
@MockKExtension.ConfirmVerification
@MockKExtension.CheckUnnecessaryStub
class BillingServiceTest {

  @MockK
  private lateinit var invoiceService: InvoiceService

  @MockK
  private lateinit var invoicePublisher: InvoicePublisher

  @InjectMockKs
  private lateinit var underTest: BillingService

  @Nested
  @DisplayName("processPending")
  inner class ProcessPending {

    private val invoices = listOf(
      Invoice(
        id = 1,
        customerId = 23,
        amount = Money(BigDecimal("120.50"), Currency.EUR),
        status = InvoiceStatus.PENDING
      ),

      Invoice(
        id = 2,
        customerId = 34,
        amount = Money(BigDecimal("140.50"), Currency.DKK),
        status = InvoiceStatus.PENDING
      )
    )

    @Test
    fun `should not publish when no invoices in PENDING status found`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns emptyList()

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { invoicePublisher wasNot Called }
    }

    @Test
    fun `should publish when invoices in PENDING status found`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns invoices
      justRun { invoicePublisher.publish(invoices[0]) }
      justRun { invoicePublisher.publish(invoices[1]) }

      // when
      assertDoesNotThrow { underTest.processPending() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
      verify(timeout = 1000) { invoicePublisher.publish(invoices[0]) }
      verify(timeout = 1000) { invoicePublisher.publish(invoices[1]) }
    }

  }

  @Nested
  @DisplayName("processRetry")
  inner class ProcessRetry {

    private val invoices = listOf(
      Invoice(
        id = 1,
        customerId = 23,
        amount = Money(BigDecimal("120.50"), Currency.EUR),
        status = InvoiceStatus.RETRY
      ),

      Invoice(
        id = 2,
        customerId = 34,
        amount = Money(BigDecimal("140.50"), Currency.DKK),
        status = InvoiceStatus.RETRY
      )
    )

    @Test
    fun `should not publish when no invoices in RETRY status found`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.RETRY) } returns emptyList()

      // when
      assertDoesNotThrow { underTest.processRetry() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.RETRY) }
      verify(timeout = 1000) { invoicePublisher wasNot Called }
    }

    @Test
    fun `should publish when invoices in RETRY status found`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.RETRY) } returns invoices
      justRun { invoicePublisher.publish(invoices[0]) }
      justRun { invoicePublisher.publish(invoices[1]) }

      // when
      assertDoesNotThrow { underTest.processRetry() }

      // then
      verify(timeout = 1000) { invoiceService.fetchByStatus(InvoiceStatus.RETRY) }
      verify(timeout = 1000) { invoicePublisher.publish(invoices[0]) }
      verify(timeout = 1000) { invoicePublisher.publish(invoices[1]) }
    }

  }

}