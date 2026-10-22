package com.example.perutours.ui.screens.admin.destination

import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination

/**
 * Reglas de validación del formulario de destinos.
 *
 * Se mantienen fuera del ViewModel para poder probarlas con JUnit y para
 * que la pantalla muestre los mismos mensajes que la lógica de guardado.
 */
object DestinationFormValidator {

    const val MAX_NAME_LENGTH = 120
    const val MAX_SUMMARY_LENGTH = 1000
    const val MAX_IMAGE_URL_LENGTH = 2048

    fun validateName(name: String): String? = when {
        name.trim().length < 3 -> "El nombre debe tener al menos 3 caracteres."
        name.trim().length > MAX_NAME_LENGTH -> "El nombre supera los $MAX_NAME_LENGTH caracteres."
        else -> null
    }

    fun validateDepartment(department: String): String? = when {
        department.trim().length < 2 -> "Indica el departamento o región."
        department.trim().length > 100 -> "El departamento supera los 100 caracteres."
        else -> null
    }

    fun validateSummary(summary: String): String? =
        if (summary.trim().length > MAX_SUMMARY_LENGTH) {
            "El resumen supera los $MAX_SUMMARY_LENGTH caracteres."
        } else {
            null
        }

    fun validateImageUrl(url: String): String? {
        val trimmed = url.trim()
        return when {
            trimmed.isEmpty() -> null
            trimmed.length > MAX_IMAGE_URL_LENGTH -> "La URL de la imagen es demasiado larga."
            !trimmed.startsWith("https://") -> "La imagen debe usar una URL https:// válida."
            else -> null
        }
    }

    /**
     * Las coordenadas son opcionales mientras se guardan borradores, pero
     * publicar un destino exige latitud y longitud dentro del rango válido.
     */
    fun parseCoordinate(raw: String): Double? =
        raw.trim().replace(',', '.').toDoubleOrNull()

    fun validateCoordinates(latitudeText: String, longitudeText: String): String? {
        val latitude = parseCoordinate(latitudeText)
        val longitude = parseCoordinate(longitudeText)
        val hasAnyValue = latitude != null || longitude != null
        return when {
            latitude == null && longitude == null -> null
            !hasAnyValue -> null
            latitude == null || longitude == null ->
                "Completa la latitud y la longitud."
            latitude < -90.0 || latitude > 90.0 ->
                "La latitud debe estar entre -90 y 90."
            longitude < -180.0 || longitude > 180.0 ->
                "La longitud debe estar entre -180 y 180."
            else -> null
        }
    }

    /** Publicar exige coordenadas válidas para consultar puntos de interés. */
    fun validateForPublish(
        latitudeText: String,
        longitudeText: String,
        externalPlaceId: String
    ): String? {
        val coordinatesError = validateCoordinates(latitudeText, longitudeText)
        if (coordinatesError != null) return coordinatesError
        val latitude = parseCoordinate(latitudeText)
        val longitude = parseCoordinate(longitudeText)
        return when {
            latitude == null || longitude == null ->
                "Para publicar necesitas validar las coordenadas del destino."
            latitude == 0.0 && longitude == 0.0 ->
                "Las coordenadas del destino todavía no fueron validadas."
            externalPlaceId.isBlank() ->
                "Valida el destino con Geoapify antes de publicarlo."
            else -> null
        }
    }

    fun parseCategories(raw: String): List<String> = raw
        .split(',', '·', '/')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(10)

    fun buildDestination(
        existing: TourDestination?,
        name: String,
        department: String,
        summary: String,
        latitudeText: String,
        longitudeText: String,
        coverImageUrl: String,
        categoriesText: String,
        externalPlaceId: String,
        status: CatalogStatus,
        createdBy: String,
        nowMillis: Long
    ): TourDestination {
        val base = existing ?: TourDestination()
        val created = base.createdAtMillis.takeIf { it > 0L } ?: nowMillis
        return base.copy(
            name = name.trim(),
            department = department.trim(),
            summary = summary.trim(),
            latitude = parseCoordinate(latitudeText) ?: 0.0,
            longitude = parseCoordinate(longitudeText) ?: 0.0,
            coverImageUrl = coverImageUrl.trim(),
            categories = parseCategories(categoriesText),
            externalPlaceId = externalPlaceId.trim(),
            status = status.name,
            createdBy = createdBy.ifBlank { base.createdBy },
            createdAtMillis = created,
            updatedAtMillis = nowMillis
        )
    }
}
