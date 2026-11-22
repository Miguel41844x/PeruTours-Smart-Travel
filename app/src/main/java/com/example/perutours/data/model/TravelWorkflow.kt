package com.example.perutours.data.model

import com.google.firebase.firestore.Exclude

/**
 * Estados canónicos del expediente de viaje.
 *
 * Los valores de [wireValue] conservan compatibilidad con los documentos que
 * ya existen en Firestore. La UI no debe volver a interpretar estados usando
 * `contains`; toda lectura pasa por [fromWire].
 */
enum class TravelRequestStatus(val wireValue: String, val label: String) {
    SUBMITTED("Pendiente de cotizacion", "Pendiente"),
    QUOTED("Cotizado", "Cotizada"),
    ACCEPTED("Aceptada", "Aceptada"),
    CHANGES_REQUESTED("Observada", "Cambios solicitados"),
    CANCELLED("Cancelada", "Cancelada"),
    UNKNOWN("", "Sin estado");

    companion object {
        fun fromWire(value: String): TravelRequestStatus = entries.firstOrNull {
            it != UNKNOWN && it.wireValue.equals(value.trim(), ignoreCase = true)
        } ?: UNKNOWN
    }
}

enum class TravelRequestType(val wireValue: String) {
    CUSTOM("CUSTOM"),
    PACKAGE("PACKAGE");

    companion object {
        fun fromWire(value: String): TravelRequestType = entries.firstOrNull {
            it.wireValue.equals(value, ignoreCase = true)
        } ?: CUSTOM
    }
}

/** Límites compartidos del formulario, catálogo y cotización. */
object TravelOfferLimits {
    const val MAX_SERVICES = 6
    const val MAX_ADDITIONAL_ACTIVITIES = 5
}

/** Snapshot inmutable del paquete tal como lo vio el cliente al solicitarlo. */
data class TravelPackageSnapshot(
    val title: String = "",
    val summary: String = "",
    val durationText: String = "",
    val durationDays: Int = 0,
    val priceAmount: Double = 0.0,
    val currency: String = "PEN",
    val coverImageUrl: String = "",
    val conditions: String = "",
    val includedServiceIds: List<String> = emptyList()
) {
    @get:Exclude
    val isPresent: Boolean
        get() = title.isNotBlank()

    companion object {
        fun from(tourPackage: TourPackage): TravelPackageSnapshot = TravelPackageSnapshot(
            title = tourPackage.title,
            summary = tourPackage.summary,
            durationText = tourPackage.durationLabel(),
            durationDays = tourPackage.durationDays,
            priceAmount = tourPackage.priceAmount,
            currency = tourPackage.currency,
            coverImageUrl = tourPackage.coverImageUrl,
            conditions = tourPackage.conditions,
            includedServiceIds = tourPackage.includedServiceIds.distinct()
        )
    }
}

enum class ReservationStatus(val wireValue: String, val label: String) {
    AWAITING_PAYMENT("Confirmada", "Pago pendiente"),
    PAYMENT_UNDER_REVIEW("Pago enviado", "Pago en revisión"),
    CONFIRMED("Pagado", "Viaje confirmado"),
    PAYMENT_REJECTED("Pago rechazado", "Comprobante observado"),
    UNKNOWN("", "Sin estado");

    companion object {
        fun fromWire(value: String): ReservationStatus = entries.firstOrNull {
            it != UNKNOWN && it.wireValue.equals(value.trim(), ignoreCase = true)
        } ?: UNKNOWN
    }
}
