package com.example.perutours.ui.screens.admin.packages

import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourPackage

/**
 * Validación del formulario de paquetes turísticos.
 * Se mantiene separada del ViewModel para poder probarla con JUnit.
 */
object PackageFormValidator {

    const val MAX_TITLE_LENGTH = 140
    const val MAX_SUMMARY_LENGTH = 1000
    const val MAX_CONDITIONS_LENGTH = 2000

    fun validateTitle(title: String): String? = when {
        title.trim().length < 3 -> "El título debe tener al menos 3 caracteres."
        title.trim().length > MAX_TITLE_LENGTH -> "El título supera los $MAX_TITLE_LENGTH caracteres."
        else -> null
    }

    fun validateDestination(destinationId: String): String? =
        if (destinationId.isBlank()) "Selecciona el destino del paquete." else null

    /** Acepta "1299", "1,299" o "S/ 1,299" y devuelve el importe numérico. */
    fun parsePrice(raw: String): Double? = raw
        .replace(Regex("[^0-9.]"), "")
        .toDoubleOrNull()

    fun validatePrice(priceText: String): String? {
        if (priceText.trim().startsWith("-")) {
            return "El precio no puede ser negativo."
        }
        val amount = parsePrice(priceText)
        return when {
            amount == null -> "Indica el precio del paquete."
            amount <= 0.0 -> "El precio debe ser mayor a cero."
            amount > 100_000_000.0 -> "El precio supera el máximo permitido."
            else -> null
        }
    }

    fun buildDurationText(days: Int, nights: Int): String {
        val safeDays = days.coerceIn(1, 365)
        val safeNights = nights.coerceAtLeast(0).coerceAtMost(safeDays)
        return "$safeDays día${if (safeDays == 1) "" else "s"} / " +
            "$safeNights noche${if (safeNights == 1) "" else "s"}"
    }

    fun validateConditions(conditions: String): String? =
        if (conditions.trim().length > MAX_CONDITIONS_LENGTH) {
            "Las condiciones superan los $MAX_CONDITIONS_LENGTH caracteres."
        } else {
            null
        }

    fun buildPackage(
        existing: TourPackage?,
        destinationId: String,
        title: String,
        summary: String,
        durationText: String,
        durationDays: Int,
        priceText: String,
        currency: String,
        ratingScore: Double,
        category: String,
        coverImageUrl: String,
        conditions: String,
        includedServiceIds: List<String>,
        status: CatalogStatus,
        createdBy: String,
        nowMillis: Long
    ): TourPackage {
        val base = existing ?: TourPackage()
        return base.copy(
            destinationId = destinationId,
            title = title.trim(),
            summary = summary.trim(),
            durationText = durationText.trim(),
            durationDays = durationDays.coerceIn(1, 365),
            priceAmount = parsePrice(priceText) ?: base.priceAmount,
            currency = currency,
            ratingScore = ratingScore.coerceIn(0.0, 5.0),
            category = category.trim().ifBlank { "Popular" },
            coverImageUrl = coverImageUrl.trim(),
            conditions = conditions.trim(),
            includedServiceIds = includedServiceIds.distinct().take(10),
            status = status.name,
            createdBy = createdBy.ifBlank { base.createdBy },
            createdAtMillis = base.createdAtMillis.takeIf { it > 0L } ?: nowMillis,
            updatedAtMillis = nowMillis
        )
    }
}
