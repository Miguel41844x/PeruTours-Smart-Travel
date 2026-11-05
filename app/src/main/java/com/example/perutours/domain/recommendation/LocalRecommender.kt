package com.example.perutours.domain.recommendation

import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.model.TravelPreferences
import com.example.perutours.data.remote.DestinationDto
import java.text.Normalizer

/**
 * HU10: recomendador local de RESPALDO (filtrado de contenido por reglas).
 *
 * Se activa cuando Gemini no está configurado, no responde o el JSON viene
 * vacío, para que la sección "Recomendado para ti" nunca quede rota.
 *
 * Puntuación por coincidencia con el perfil del usuario:
 *  - Tipo de viaje vs categoría del destino      → +4
 *  - Categoría de interés vs categoría           → +3
 *  - Presupuesto vs precio real del paquete      → +3
 *  - Duración vs días del paquete                → +3
 *  - Valoración de viajeros (0 a 5)              → +0.5
 */
object LocalRecommender {

    // Tramos de presupuesto por persona (soles) para cada etiqueta.
    // Triplets: mínimo aceptable y máximo aceptable (Double.MAX_VALUE = sin tope).
    private val budgetRanges = mapOf(
        "Económico" to (0.0..800.0),
        "Medio" to (801.0..1500.0),
        "Premium" to (1501.0..Double.MAX_VALUE)
    )

    private fun budgetRange(budget: String): ClosedFloatingPointRange<Double>? =
        budgetRanges[budget]

    fun recommend(
        preferences: TravelPreferences,
        candidates: List<DestinationDto>,
        max: Int = 5
    ): List<Recommendation> {
        if (candidates.isEmpty()) return emptyList()

        return candidates
            .map { destination -> score(destination, preferences) }
            .sortedWith(compareByDescending<Scored> { it.score }.thenByDescending { it.rating })
            .take(max)
            .map { it.recommendation }
    }

    private data class Scored(
        val score: Double,
        val rating: Double,
        val recommendation: Recommendation
    )

    private fun score(
        destination: DestinationDto,
        preferences: TravelPreferences
    ): Scored {
        val haystack = normalize(
            listOf(destination.category, destination.title, destination.location)
                .joinToString(" ")
        )
        val category = normalize(destination.category)
        val factors = mutableListOf<String>()
        var score = 0.0

        // 1. Tipo de viaje (Aventura, Cultura, Relax...) vs categoría
        val tripType = normalize(preferences.tripType)
        val tripTypeMatch = tripType.isNotBlank() &&
            (category.contains(tripType) || haystack.contains(tripType))
        if (tripTypeMatch) {
            score += 4
            factors.add("tu tipo de viaje ${preferences.tripType}")
        }

        // 2. Categorías de interés vs categoría del destino
        val interest = preferences.interestNames
            .firstOrNull { normalize(it).let { name -> name.isNotBlank() && category.contains(name) } }
            ?: preferences.interestNames
                .firstOrNull { normalize(it).let { name -> name.isNotBlank() && haystack.contains(name) } }
        if (interest != null) {
            score += 3
            factors.add("tu interés en $interest")
        }

        // 3. Presupuesto vs precio real del paquete
        val price = parsePrice(destination.price)
        val budgetRange = budgetRange(preferences.budget)
        if (price != null && budgetRange != null) {
            if (price >= budgetRange.start && price <= budgetRange.endInclusive) {
                score += 3
                factors.add("tu presupuesto ${preferences.budget}")
            } else if (price >= budgetRange.start - 400 && price <= budgetRange.endInclusive + 400) {
                // Cerca del tramo elegido: bonificación parcial.
                score += 1
            }
        }

        // 4. Duración vs días reales del paquete
        val days = parseDays(destination.duration)
        if (days != null && fitsDuration(days, preferences.duration)) {
            score += 3
            factors.add("la duración que buscas (${preferences.duration})")
        }

        // 5. Valoración de viajeros
        val rating = destination.rating.toDoubleOrNull() ?: 0.0
        score += rating * 0.5

        val reason = when {
            factors.isNotEmpty() -> "Encaja con ${factors.take(2).joinToString(" y ")}."
            else -> "Buena opción según tu perfil y la valoración de otros viajeros."
        }

        return Scored(
            score = score,
            rating = rating,
            recommendation = Recommendation(
                destinationId = destination.destinationId.ifBlank { destination.id },
                title = destination.title,
                reason = reason,
                source = RecommendationSource.LOCAL,
                destination = destination
            )
        )
    }

    /** "S/ 1,299" → 1299.0 */
    internal fun parsePrice(price: String): Double? {
        val digits = price.replace(Regex("[^0-9.]"), "")
        return digits.takeIf { it.isNotBlank() }?.toDoubleOrNull()
    }

    /** "4 días / 3 noches" → 4 */
    internal fun parseDays(duration: String): Int? =
        Regex("(\\d+)\\s*d[ií]a").find(normalize(duration))?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("^(\\d+)").find(duration.trim())?.groupValues?.get(1)?.toIntOrNull()

    internal fun fitsDuration(days: Int, wantedDuration: String): Boolean =
        when (wantedDuration) {
            "Fin de semana" -> days <= 3
            "3-5 días" -> days in 3..5
            "1 semana" -> days in 5..8
            "2+ semanas" -> days >= 8
            else -> true
        }

    /** Minúsculas sin acentos para comparar "Gastronomía" con "gastronomia". */
    internal fun normalize(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .lowercase()
            .trim()
}
