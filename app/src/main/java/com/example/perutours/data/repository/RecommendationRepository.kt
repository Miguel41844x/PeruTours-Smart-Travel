package com.example.perutours.data.repository

import com.example.perutours.BuildConfig
import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.model.TravelPreferences
import com.example.perutours.data.remote.gemini.GeminiClient
import com.example.perutours.data.remote.gemini.GeminiApiService
import com.example.perutours.domain.recommendation.LocalRecommender
import com.example.perutours.domain.recommendation.RecommendationPromptBuilder
import com.example.perutours.domain.recommendation.RecommendationResponseParser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * HU10: servicio de recomendación inteligente.
 *
 * Flujo (todo con corrutinas en Dispatchers.IO):
 *  1. Lee las preferencias del usuario desde Firestore (HU10).
 *  2. Toma el catálogo de destinos desde Room (Single Source of Truth).
 *  3. Construye el prompt y llama a Gemini con Retrofit.
 *  4. Parsea el JSON de respuesta y lo une con el catálogo real.
 *
 * La clave de Gemini puede venir vacía (sin configurar); en ese caso, o
 * ante cualquier fallo de la IA, se aplica automáticamente el recomendador
 * local de respaldo (LocalRecommender) para no dejar la sección vacía.
 */
class RecommendationRepository(
    private val auth: FirebaseAuth,
    private val profileRepository: ProfileRepository,
    private val destinationRepository: DestinationRepository,
    private val geminiApi: GeminiApiService
) {

    /**
     * Recomendaciones personalizadas para el usuario autenticado.
     * @param forceRefresh ignora la caché de Firestore (commit de caché).
     */
    suspend fun getRecommendations(
        forceRefresh: Boolean = false
    ): Result<List<Recommendation>> = withContext(Dispatchers.IO) {
        try {
            val uid = auth.currentUser?.uid.orEmpty()
            if (uid.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("No hay un usuario autenticado.")
                )
            }

            // 1. Preferencias del usuario (Firestore: perfil + intereses)
            val preferences = loadTravelPreferences(uid)

            // 2. Catálogo candidato desde Room (ya sincronizado por Home)
            destinationRepository.refreshDestinations(forceRefresh = false)
            val candidates = destinationRepository.localDestinations
                .first()
                .map { it.toDto() }

            if (candidates.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // 3. IA Gemini con respaldo local: la sección nunca queda vacía
            val recommendations = try {
                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    throw GeminiUnavailableException()
                }

                val request = RecommendationPromptBuilder.toRequest(preferences, candidates)
                val response = geminiApi.generateContent(
                    model = GeminiClient.MODEL,
                    request = request
                )

                val entries = RecommendationResponseParser.parse(response.text())
                mapToCatalog(entries, candidates).ifEmpty {
                    LocalRecommender.recommend(preferences, candidates)
                }
            } catch (_: Exception) {
                // Sin clave, sin red o con respuesta inválida de la IA:
                // filtrado de contenido local (siempre disponible).
                LocalRecommender.recommend(preferences, candidates)
            }

            Result.success(recommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Perfil estructurado + nombres de las categorías de interés marcadas. */
    suspend fun loadTravelPreferences(uid: String): TravelPreferences {
        val profile = profileRepository.getProfile(uid)

        val interestNames = try {
            val catalog = profileRepository.getPreferences()
            catalog
                .filter { it.id in profile.preferences }
                .map { it.name }
        } catch (_: Exception) {
            emptyList()
        }

        return TravelPreferences(
            tripType = profile.tripType,
            budget = profile.budget,
            duration = profile.duration,
            interestNames = interestNames
        )
    }

    /**
     * Une la respuesta de la IA con el catálogo: descarta destinos que ya
     * no existen, usa el título real del catálogo y limita el resultado.
     */
    private fun mapToCatalog(
        entries: List<com.example.perutours.data.remote.gemini.GeminiRecommendedDestination>,
        candidates: List<com.example.perutours.data.remote.DestinationDto>
    ): List<Recommendation> {
        val byId = candidates.associateBy { it.destinationId.ifBlank { it.id } }

        return entries
            .mapNotNull { entry ->
                val destination = byId[entry.destinationId] ?: return@mapNotNull null
                Recommendation(
                    destinationId = entry.destinationId,
                    title = destination.title,
                    reason = entry.reason
                        .ifBlank { "Coincide con tus preferencias de viaje." }
                        .take(160),
                    source = RecommendationSource.AI,
                    destination = destination
                )
            }
            .distinctBy { it.destinationId }
            .take(MAX_RECOMMENDATIONS)
    }

    /** La clave de Gemini no está configurada (debe usar el fallback local). */
    class GeminiUnavailableException :
        IllegalStateException("La clave de Gemini no está configurada.")

    companion object {
        const val MAX_RECOMMENDATIONS = 5
    }
}
