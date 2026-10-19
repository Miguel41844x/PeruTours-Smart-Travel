package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.QuotationServiceItem

object QuotationValidator {
    const val MAX_CONDITIONS_LENGTH = 2000

    /**
     * Tope de servicios por cotización. Debe coincidir con el número de
     * posiciones que inspecciona `isValidQuotationServices` en
     * firestore.rules (5): el evaluador de reglas Firestore tiene un techo
     * de 1000 expresiones por solicitud y cada posición inspeccionada cuesta
     * expresiones en cada escritura de cotización.
     */
    const val MAX_SERVICES = 5

    fun validateDestination(destination: String): String? {
        if (destination.trim().isEmpty()) {
            return "El destino de la cotización es obligatorio."
        }
        if (destination.trim().length < 3) {
            return "El destino debe tener al menos 3 caracteres."
        }
        return null
    }

    fun validateClientName(clientName: String): String? {
        if (clientName.trim().isEmpty()) {
            return "El nombre del cliente no puede estar vacío."
        }
        return null
    }

    fun validateServices(services: List<QuotationServiceItem>): String? {
        if (services.isEmpty()) {
            return "Debes incluir al menos un servicio turístico en la propuesta."
        }
        if (services.size > MAX_SERVICES) {
            return "La cotización puede incluir hasta $MAX_SERVICES servicios. " +
                "Combina los servicios restantes en uno más general."
        }
        val hasInvalidCost = services.any { it.unitCost <= 0.0 || it.quantity <= 0 }
        if (hasInvalidCost) {
            return "Todos los servicios deben tener un costo unitario y cantidad mayor a cero."
        }
        return null
    }

    fun validateMargin(marginPercent: Int): String? {
        if (marginPercent < 5 || marginPercent > 50) {
            return "El margen comercial debe ubicarse entre 5% y 50%."
        }
        return null
    }

    fun validateValidityDays(days: Int): String? {
        if (days < 1 || days > 30) {
            return "La vigencia debe encontrarse entre 1 y 30 días."
        }
        return null
    }

    fun validateConditions(conditions: String): String? {
        return if (conditions.length > MAX_CONDITIONS_LENGTH) {
            "Las condiciones no pueden superar $MAX_CONDITIONS_LENGTH caracteres."
        } else {
            null
        }
    }
}
