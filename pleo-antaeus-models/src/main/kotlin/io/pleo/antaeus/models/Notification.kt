package io.pleo.antaeus.models

data class Notification(
  val customerId: Int,
  val invoiceId: Int,
  val text: String
)
