package com.example.perutours.data.model

/**
 * HU07: Entidad independiente del pago. Se almacena en payments/{paymentId}
 * Cada envío es un intento independiente para conservar el historial de
 * rechazos y reenvíos. La reserva apunta únicamente al intento vigente.
 */
data class Payment(
    val id: String = "",
    val reservationId: String = "",
    val quotationId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val attemptNumber: Int = 1,
    val previousPaymentId: String = "",
    val method: String = "",
    val amount: Double = 0.0,
    val currency: String = "PEN",
    val voucherUrl: String = "",
    val status: String = STATUS_PENDING,
    val rejectionReason: String = "",
    val submittedAtMillis: Long = 0L,
    val reviewedAtMillis: Long? = null,
    val reviewedBy: String = ""
) {
    companion object {
        const val STATUS_PENDING = "Pendiente"
        const val STATUS_APPROVED = "Aprobado"
        const val STATUS_REJECTED = "Rechazado"
    }
}
