package io.pleo.antaeus.core.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class SchedulerService(
  private val billingService: BillingService,
  /**
   * Function that returns the delay in seconds between each execution
   */
  private val delaySupplier: () -> Long
) {

  companion object {
    private const val MILLISECONDS_IN_SEC = 1000
  }

  private val log = KotlinLogging.logger {}

  fun start() {
    log.info { "Starting SchedulerService" }
    CoroutineScope(Dispatchers.Default).launch {
      while (true) {
        delay(delaySupplier() * MILLISECONDS_IN_SEC)
        launch { billingService.performBilling() }
        log.info { "Launched billing process" }
      }
    }
  }

}