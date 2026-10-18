package com.example.perutours.data.model

/**
 * HU07: Entidad independiente del pago. Se almacena en payments/{paymentId}
 * usando el ID de la reserva como identificador para evitar duplicados.
 * La reserva solo conserva su estado general y el paymentId asociado.
 */
data class Payment(
    val id: String = "",
    val reservationId: String = "",
    val quotationId: String = "",
    val clientId: String = "",
    val agentId: String = "",
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
