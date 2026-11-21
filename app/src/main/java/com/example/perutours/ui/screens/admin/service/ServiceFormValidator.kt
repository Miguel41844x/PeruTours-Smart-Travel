package com.example.perutours.ui.screens.admin.service

object ServiceFormValidator {
    fun validate(
        name: String,
        destinationId: String,
        providerId: String,
        category: String,
        unitCost: Double?,
        description: String,
        durationMinutes: Int?
    ): String? = when {
        name.trim().length !in 3..120 -> "El nombre debe tener entre 3 y 120 caracteres."
        destinationId.isBlank() -> "Selecciona el destino del servicio."
        providerId.isBlank() -> "Selecciona el proveedor responsable."
        category.isBlank() -> "Selecciona una categoría."
        unitCost == null || !unitCost.isFinite() || unitCost <= 0.0 -> {
            "Ingresa un costo válido mayor que cero."
        }
        unitCost > 1_000_000.0 -> "El costo supera el máximo permitido."
        description.length > 1_000 -> "La descripción no puede superar 1000 caracteres."
        durationMinutes == null || durationMinutes !in 15..10_080 -> {
            "La duración debe estar entre 15 minutos y 7 días."
        }
        else -> null
    }
}
