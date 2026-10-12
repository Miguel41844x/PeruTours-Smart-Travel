package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationValidity

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

    fun validateAcceptance(
        quotation: Quotation,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        return when {
            !Quotation.canClientRespond(quotation.status) ->
                "La cotización ya fue respondida."
            QuotationValidity.validUntilExclusiveMillis(quotation) == null ->
                "La cotización no tiene una fecha de vigencia válida."
            QuotationValidity.isExpired(quotation, nowMillis) ->
                "La cotización venció. Solicita una propuesta actualizada."
            else -> null
        }
    }
}
