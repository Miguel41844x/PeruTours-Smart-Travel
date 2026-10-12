package com.example.perutours.ui.screens.request

data class TravelRequestValidationResult(
    val destinationError: String? = null,
    val originCityError: String? = null,
    val departureError: String? = null,
    val returnError: String? = null,
    val travelerCountError: String? = null,
    val notesError: String? = null
) {
    val isValid: Boolean
        get() = destinationError == null &&
            originCityError == null &&
            departureError == null &&
            returnError == null &&
            travelerCountError == null &&
            notesError == null
}

object TravelRequestValidator {
    const val MAX_PLACE_LENGTH = 120
    const val MAX_NOTES_LENGTH = 500
    const val MIN_TRAVELER_COUNT = 1
    const val MAX_TRAVELER_COUNT = 20

    fun validate(
        destination: String,
        originCity: String,
        departureAtMillis: Long,
        returnAtMillis: Long,
        travelerCount: Int,
        notes: String = "",
        nowMillis: Long = System.currentTimeMillis()
    ): TravelRequestValidationResult {
        return TravelRequestValidationResult(
            destinationError = when (destination.trim().length) {
                in 0..2 -> "Ingresa un destino válido."
                in (MAX_PLACE_LENGTH + 1)..Int.MAX_VALUE ->
                    "El destino no puede superar $MAX_PLACE_LENGTH caracteres."
                else -> null
            },
            originCityError = when (originCity.trim().length) {
                in 0..1 -> "Ingresa la ciudad de origen."
                in (MAX_PLACE_LENGTH + 1)..Int.MAX_VALUE ->
                    "La ciudad de origen no puede superar $MAX_PLACE_LENGTH caracteres."
                else -> null
            },
            departureError = if (departureAtMillis <= nowMillis) {
                "Selecciona una fecha y hora de salida futura."
            } else {
                null
            },
            returnError = if (returnAtMillis <= departureAtMillis) {
                "El retorno debe ser posterior a la salida."
            } else {
                null
            },
            travelerCountError = if (travelerCount !in MIN_TRAVELER_COUNT..MAX_TRAVELER_COUNT) {
                "La cantidad de viajeros debe estar entre $MIN_TRAVELER_COUNT y $MAX_TRAVELER_COUNT."
            } else {
                null
            },
            notesError = if (notes.trim().length > MAX_NOTES_LENGTH) {
                "Las notas no pueden superar $MAX_NOTES_LENGTH caracteres."
            } else {
                null
            }
        )
    }
}
