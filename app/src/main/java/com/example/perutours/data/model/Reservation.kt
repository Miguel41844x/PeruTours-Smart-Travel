package com.example.perutours.data.model

data class Reservation(
    val id: String = "",
    val quotationId: String = "",
    val requestId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val destination: String = "",
    val originCity: String = "",
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val services: List<QuotationServiceItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val currency: String = "USD",
    val validityDate: String = "",
    val conditions: String = "",
    val status: String = STATUS_CONFIRMED,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val paymentVoucherUrl: String = "",
    val paymentMethod: String = "",
    val paidAtMillis: Long? = null
) {
    companion object {
        const val STATUS_CONFIRMED = "Confirmada"
        const val STATUS_PAID = "Pagado"
    }
}
