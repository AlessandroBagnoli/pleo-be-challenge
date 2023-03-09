package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.InvoiceStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@MockKExtension.ConfirmVerification
@MockKExtension.CheckUnnecessaryStub
@ExtendWith(MockKExtension::class)
class BillingServiceTest {

  @MockK
  private lateinit var paymentProvider: PaymentProvider

  @MockK
  private lateinit var invoiceService: InvoiceService

  @InjectMockKs
  private lateinit var underTest: BillingService

  @Nested
  @DisplayName("performBilling")
  inner class PerformBilling {

    @Test
    fun `should do nothing when no invoices in pending status`() {
      // given
      every { invoiceService.fetchByStatus(InvoiceStatus.PENDING) } returns emptyList()

      // when
      assertDoesNotThrow { underTest.performBilling() }

      // then
      verify { invoiceService.fetchByStatus(InvoiceStatus.PENDING) }
    }

  }


}