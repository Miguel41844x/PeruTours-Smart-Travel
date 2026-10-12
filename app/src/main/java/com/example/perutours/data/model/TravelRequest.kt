package com.example.perutours.data.model

data class TravelRequest(
    val id: String = "",
    val userId: String = "",
    val destination: String = "",
    val originCity: String = "",
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val notes: String = "",
    val status: String = STATUS_PENDING_QUOTE,
    val createdAtMillis: Long = 0L
) {
    companion object {
        const val STATUS_PENDING_QUOTE = "Pendiente de cotizacion"
    }
}
