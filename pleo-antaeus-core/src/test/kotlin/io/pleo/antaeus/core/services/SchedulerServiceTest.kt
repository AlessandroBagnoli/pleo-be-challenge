package io.pleo.antaeus.core.services

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
@MockKExtension.CheckUnnecessaryStub
class SchedulerServiceTest {

  @MockK
  private lateinit var billingService: BillingService

  @MockK
  private lateinit var delaySupplier: () -> Long

  @InjectMockKs
  private lateinit var underTest: SchedulerService

  @Nested
  @DisplayName("start")
  inner class Start {

    @Test
    fun `should call performBilling`() {
      // given
      every { delaySupplier() } returns 1
      coJustRun { billingService.performBilling() }

      // when
      assertDoesNotThrow { underTest.start() }

      // then
      verify { delaySupplier() }
      coVerify(timeout = 1500) { billingService.performBilling() }
    }

    @Test
    fun `should not throw exception if performBilling throws exception`() {
      // given
      every { delaySupplier() } returns 1
      coEvery { billingService.performBilling() } throws RuntimeException("some exception")

      // when
      assertDoesNotThrow { underTest.start() }

      // then
      verify { delaySupplier() }
      coVerify(timeout = 1500) { billingService.performBilling() }
    }

  }

}