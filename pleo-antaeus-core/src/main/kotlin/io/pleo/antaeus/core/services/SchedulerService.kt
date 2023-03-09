package io.pleo.antaeus.core.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

class SchedulerService(
  private val billingService: BillingService
) {

  private val log = KotlinLogging.logger {}

  fun start() {
    log.info { "Starting SchedulerService" }
    CoroutineScope(Dispatchers.Default).launch {
      while (true) {
        delay(1000) // Wait until the first day of next month
        // Code to run every first of the month goes here
        log.info { "Running task on the first of the month" }
        billingService.performBilling()
      }
    }
  }

}