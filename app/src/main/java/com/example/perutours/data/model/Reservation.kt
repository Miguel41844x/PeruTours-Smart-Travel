package com.example.perutours.data.model

data class Reservation(
    val id: String = "",
    val quotationId: String = "",
    val requestId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val destination: String = "",
    val travelerCount: Int = 1,
    val totalAmount: Double = 0.0,
    val currency: String = "USD",
    val status: String = STATUS_CONFIRMED,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_CONFIRMED = "Confirmada"
    }
}
