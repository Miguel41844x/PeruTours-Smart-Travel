package com.example.perutours.data.model

import com.google.firebase.firestore.Exclude

data class Reservation(
    val id: String = "",
    val quotationId: String = "",
    val requestId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val packageId: String = "",
    val destinationId: String = "",
    val packageSnapshot: TravelPackageSnapshot = TravelPackageSnapshot(),
    val destination: String = "",
    val originCity: String = "",
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val services: List<QuotationServiceItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val currency: String = "PEN",
    val validityDate: String = "",
    val conditions: String = "",
    val status: String = STATUS_CONFIRMED,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val paymentId: String = "",
    // Compatibilidad de lectura con reservas antiguas. Los pagos nuevos se
    // gestionan en la colección payments.
    val paymentVoucherUrl: String = "",
    val paymentMethod: String = "",
    val paidAtMillis: Long? = null
) {
    @get:Exclude
    val workflowStatus: ReservationStatus
        get() = ReservationStatus.fromWire(status)

    companion object {
        const val STATUS_CONFIRMED = "Confirmada"
        const val STATUS_PAYMENT_PENDING = "Pago enviado"
        const val STATUS_PAID = "Pagado"
        const val STATUS_PAYMENT_REJECTED = "Pago rechazado"
    }
}
