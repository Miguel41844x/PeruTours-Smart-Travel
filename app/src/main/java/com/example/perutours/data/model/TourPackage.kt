package com.example.perutours.data.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Paquete turístico vendible vinculado a un destino.
 *
 * Colección Firestore: packages/{packageId}
 * Concentra lo que PeruTours vende: precio, duración, categoría, imagen y
 * condiciones comerciales. La lista [includedServiceIds] guarda los IDs de
 * los servicios incluidos (catálogo interno de servicios).
 */
data class TourPackage(
    val id: String = "",
    val destinationId: String = "",
    val title: String = "",
    val summary: String = "",
    val durationText: String = "",
    val durationDays: Int = 1,
    val priceAmount: Double = 0.0,
    val currency: String = "PEN",
    val ratingScore: Double = 0.0,
    val category: String = "Popular",
    val coverImageUrl: String = "",
    val conditions: String = "",
    val includedServiceIds: List<String> = emptyList(),
    val status: String = CatalogStatus.BORRADOR.name,
    val createdBy: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val published: Boolean
        get() = CatalogStatus.from(status) == CatalogStatus.PUBLICADO

    /** Texto mostrado en la tarjeta del cliente, p. ej. "S/ 1,299". */
    fun priceLabel(): String = formatCatalogPrice(priceAmount, currency)

    fun durationLabel(): String = durationText.ifBlank {
        "$durationDays día${if (durationDays == 1) "" else "s"}"
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "destinationId" to destinationId,
        "title" to title,
        "summary" to summary,
        "durationText" to durationText,
        "durationDays" to durationDays,
        "priceAmount" to priceAmount,
        "currency" to currency,
        "ratingScore" to ratingScore,
        "category" to category,
        "coverImageUrl" to coverImageUrl,
        "conditions" to conditions,
        "includedServiceIds" to includedServiceIds,
        "status" to status,
        "createdBy" to createdBy,
        "createdAtMillis" to createdAtMillis,
        "updatedAtMillis" to updatedAtMillis
    )
}

internal fun formatCatalogPrice(amount: Double, currency: String): String {
    val formatted = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }.format(amount).replace(',', ' ')
    return if (currency == "USD") "USD $formatted" else "S/ $formatted"
}
