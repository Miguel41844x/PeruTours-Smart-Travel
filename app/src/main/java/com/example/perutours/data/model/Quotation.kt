package com.example.perutours.data.model

data class QuotationServiceItem(
    val id: String = "",
    val name: String = "",
    val category: String = "", // Alojamiento, Transporte, Tours, Guía, Seguro, Alimentación
    val unitCost: Double = 0.0,
    val quantity: Int = 1,
    val subtotal: Double = unitCost * quantity
)

data class Quotation(
    val id: String = "",
    val requestId: String = "",            // ID del documento en travel_requests
    val clientId: String = "",             // UID del cliente en Firebase Auth
    val clientName: String = "",
    val clientEmail: String = "",
    val destination: String = "",
    val travelerCount: Int = 1,
    val services: List<QuotationServiceItem> = emptyList(),
    val baseCost: Double = 0.0,
    val marginPercent: Int = 15,
    val totalAmount: Double = 0.0,
    val currency: String = "USD",
    val validityDate: String = "",         // Formato YYYY-MM-DD
    val conditions: String = "",
    val status: String = STATUS_QUOTED,    // ESTADO OBLIGATORIO: 'Cotizado'
    val agentId: String = "",
    val agentName: String = "Nataly Chávez",
    val clientFcmToken: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_QUOTED = "Cotizado"
        const val STATUS_ACCEPTED = "Aceptada"
        const val STATUS_OBSERVED = "Observada"
        const val STATUS_REJECTED = "Rechazada"
    }
}
