package com.example.perutours.data.model

/**
 * Opciones de preferencias de viaje de la HU10 (registro de preferencias):
 * tipo de viaje, presupuesto y duración.
 *
 * Los valores son EXACTAMENTE los que permiten las reglas de Firestore
 * (firestore.rules → isValidFullUser) para que el guardado siempre sea
 * válido y el recomendador local pueda comparar sin ambigüedades.
 */
object TravelProfileOptions {

    val tripTypes = listOf(
        "Aventura",
        "Cultura",
        "Relax",
        "Gastronomía",
        "Naturaleza",
        "Familiar"
    )

    val budgets = listOf(
        "Económico",
        "Medio",
        "Premium"
    )

    val durations = listOf(
        "Fin de semana",
        "3-5 días",
        "1 semana",
        "2+ semanas"
    )

    /** Cadena vacía = preferencia aún no registrada (opcional en el perfil). */
    fun isValidTripType(value: String): Boolean =
        value.isBlank() || value in tripTypes

    fun isValidBudget(value: String): Boolean =
        value.isBlank() || value in budgets

    fun isValidDuration(value: String): Boolean =
        value.isBlank() || value in durations
}
