package com.example.perutours.ui.screens.quotation

object QuotationResponseValidator {

    const val MIN_OBSERVATION_LENGTH = 10
    const val MAX_OBSERVATION_LENGTH = 500

    fun validateObservation(message: String): String? {
        val cleanMessage = message.trim()
        return when {
            cleanMessage.isEmpty() -> "Escribe una observación antes de enviarla."
            cleanMessage.length < MIN_OBSERVATION_LENGTH ->
                "La observación debe tener al menos $MIN_OBSERVATION_LENGTH caracteres."
            cleanMessage.length > MAX_OBSERVATION_LENGTH ->
                "La observación no puede superar los $MAX_OBSERVATION_LENGTH caracteres."
            else -> null
        }
    }
}
