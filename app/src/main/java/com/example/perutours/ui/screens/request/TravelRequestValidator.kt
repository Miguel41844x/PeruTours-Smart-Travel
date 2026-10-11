package com.example.perutours.ui.screens.request

data class TravelRequestValidationResult(
    val destinationError: String? = null,
    val originCityError: String? = null,
    val departureError: String? = null,
    val returnError: String? = null,
    val travelerCountError: String? = null
) {
    val isValid: Boolean
        get() = destinationError == null &&
            originCityError == null &&
            departureError == null &&
            returnError == null &&
            travelerCountError == null
}

object TravelRequestValidator {
    fun validate(
        destination: String,
        originCity: String,
        departureAtMillis: Long,
        returnAtMillis: Long,
        travelerCount: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): TravelRequestValidationResult {
        return TravelRequestValidationResult(
            destinationError = when (destination.trim().length) {
                in 0..2 -> "Ingresa un destino válido."
                in 121..Int.MAX_VALUE -> "El destino no puede superar 120 caracteres."
                else -> null
            },
            originCityError = when (originCity.trim().length) {
                in 0..1 -> "Ingresa la ciudad de origen."
                in 121..Int.MAX_VALUE -> "La ciudad de origen no puede superar 120 caracteres."
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
            travelerCountError = if (travelerCount !in 1..20) {
                "La cantidad de viajeros debe estar entre 1 y 20."
            } else {
                null
            }
        )
    }
}
