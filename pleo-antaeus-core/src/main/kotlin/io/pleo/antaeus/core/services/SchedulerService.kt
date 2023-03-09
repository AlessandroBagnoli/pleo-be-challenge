package io.pleo.antaeus.core.services

import mu.KotlinLogging

class SchedulerService(
  billingService: BillingService
) {

  private val log = KotlinLogging.logger {}

  fun start() {
    log.info { "Starting SchedulerService" }
  }

}