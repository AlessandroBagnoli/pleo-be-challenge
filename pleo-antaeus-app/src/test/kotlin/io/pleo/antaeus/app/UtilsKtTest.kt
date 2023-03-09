package io.pleo.antaeus.app

import everyFirstDayOfTheMonth
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verifyAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockKExtension::class)
@MockKExtension.ConfirmVerification
@MockKExtension.CheckUnnecessaryStub
class UtilsKtTest {

  @MockK
  private lateinit var clock: Clock

  @Test
  fun `everyFirstDayOfTheMonth should return the number of seconds between now and the first day of next month`() {
    // given
    val mockedNow = Instant.parse("2007-12-03T10:15:30.00Z")
    every { clock.instant() } returns mockedNow
    every { clock.zone } returns ZoneOffset.UTC

    // when
    val actual = everyFirstDayOfTheMonth(clock)

    // then
    verifyAll {
      clock.instant()
      clock.zone
    }
    val nextMonth = Instant.parse("2008-01-01T00:00:00.00Z")
    val expected = Duration.between(mockedNow, nextMonth).toSeconds()
    assertEquals(expected, actual)
  }
}