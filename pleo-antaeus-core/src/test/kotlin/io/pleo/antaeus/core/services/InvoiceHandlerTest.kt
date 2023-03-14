package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import io.mockk.verifyAll
import io.pleo.antaeus.core.channel.outbound.NotificationPublisher
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.*
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
class InvoiceHandlerTest {

  @MockK
  private lateinit var paymentProvider: PaymentProvider

  @MockK
  private lateinit var invoiceService: InvoiceService

  @MockK
  private lateinit var notificationPublisher: NotificationPublisher

  @InjectMockKs
  private lateinit var underTest: InvoiceHandler

  private val dummyInvoice = Invoice(
    id = 1,
    customerId = 1,
    amount = Money(BigDecimal("120.50"), Currency.EUR),
    status = InvoiceStatus.PENDING
  )

  private val PAID_TEXT = "Your invoice has been paid!"
  private val FAILED_TEXT = "An error has occurred during the payment of your invoice"

  @Nested
  @DisplayName("process")
  inner class Process {

    @Test
    fun `should set status to PAID and send notification when provider charges correctly`() {
      // given
      every { paymentProvider.charge(dummyInvoice) } returns true
      every { invoiceService.updateStatus(1, InvoiceStatus.PAID) } returns 1
      val notification =
        Notification(invoiceId = dummyInvoice.id, customerId = dummyInvoice.customerId, text = PAID_TEXT)
      justRun { notificationPublisher.publish(notification) }

      // when
      assertDoesNotThrow { underTest.process(dummyInvoice) }

      // then
      verifyAll {
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.PAID)
        notificationPublisher.publish(notification)
      }
    }

    @Test
    fun `should set status to RETRY when provider does not charge`() {
      // given
      every { paymentProvider.charge(dummyInvoice) } returns false
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.process(dummyInvoice) }

      // then
      verifyAll {
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.RETRY)
      }
    }

    @ParameterizedTest
    @MethodSource("io.pleo.antaeus.core.services.InvoiceHandlerTest#exceptions for FAILED")
    fun `should set status to FAILED and send notification when provider throws CurrencyMismatchException, CustomerNotFoundException, or some unhandled exception`(
      exception: Exception
    ) {
      // given
      every { paymentProvider.charge(dummyInvoice) } throws exception
      every { invoiceService.updateStatus(1, InvoiceStatus.FAILED) } returns 1
      val notification =
        Notification(invoiceId = dummyInvoice.id, customerId = dummyInvoice.customerId, text = FAILED_TEXT)
      justRun { notificationPublisher.publish(notification) }

      // when
      assertDoesNotThrow { underTest.process(dummyInvoice) }

      // then
      verifyAll {
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.FAILED)
        notificationPublisher.publish(notification)
      }
    }

    @Test
    fun `should set status to RETRY when provider throws NetworkException or any other generic exception`() {
      // given
      every { paymentProvider.charge(dummyInvoice) } throws NetworkException()
      every { invoiceService.updateStatus(1, InvoiceStatus.RETRY) } returns 1

      // when
      assertDoesNotThrow { underTest.process(dummyInvoice) }

      // then
      verifyAll {
        paymentProvider.charge(dummyInvoice)
        invoiceService.updateStatus(1, InvoiceStatus.RETRY)
      }
    }
  }

  companion object {
    @JvmStatic
    fun `exceptions for FAILED`() = listOf(
      Arguments.of(CurrencyMismatchException(invoiceId = 1, customerId = 1)),
      Arguments.of(CustomerNotFoundException(id = 1)),
      Arguments.of(RuntimeException("some random exception"))
    )
  }

}