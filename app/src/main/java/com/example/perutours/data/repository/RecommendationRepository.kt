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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val firestore: FirebaseFirestore,
    private val profileRepository: ProfileRepository,
    private val destinationRepository: DestinationRepository,
    private val geminiApi: GeminiApiService
) {

    /**
     * Recomendaciones personalizadas para el usuario autenticado.
     * @param forceRefresh ignora la caché de Firestore y llama a la IA.
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

            // 2b. Caché en Firestore: evita gastar cuota de IA si el
            //     perfil no cambió y la caché sigue vigente.
            if (!forceRefresh) {
                readCache(uid, preferences, candidates)?.let { cached ->
                    return@withContext Result.success(cached)
                }
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

            // 4. Caché best-effort del resultado para próximas aperturas
            saveCache(uid, preferences, recommendations)

            Result.success(recommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===================== Caché en Firestore (HU10) =====================

    /** Último resultado guardado si el perfil no cambió y sigue vigente. */
    private suspend fun readCache(
        uid: String,
        preferences: TravelPreferences,
        candidates: List<com.example.perutours.data.remote.DestinationDto>
    ): List<Recommendation>? {
        return try {
            val document = firestore
                .collection("users")
                .document(uid)
                .collection(RECOMMENDATIONS_COLLECTION)
                .document(CACHE_DOCUMENT)
                .get()
                .await()

            if (!document.exists()) return null

            val updatedAt = document.getLong("updatedAt") ?: return null
            if (System.currentTimeMillis() - updatedAt > CACHE_TTL_MILLIS) return null

            // Si el usuario cambió sus preferencias, la caché no sirve.
            if (document.getString("tripType") != preferences.tripType ||
                document.getString("budget") != preferences.budget ||
                document.getString("duration") != preferences.duration
            ) {
                return null
            }

            val cachedInterests = (document.get("interests") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
            if (cachedInterests != preferences.interestNames) return null

            @Suppress("UNCHECKED_CAST")
            val items = document.get("items") as? List<Map<String, Any?>> ?: return null
            val byId = candidates.associateBy { it.destinationId.ifBlank { it.id } }

            items.mapNotNull { item ->
                val destinationId = item["destinationId"] as? String
                    ?: return@mapNotNull null
                val destination = byId[destinationId]
                    ?: return@mapNotNull null

                Recommendation(
                    destinationId = destinationId,
                    title = (item["title"] as? String)?.takeIf { it.isNotBlank() }
                        ?: destination.title,
                    reason = item["reason"] as? String ?: "",
                    source = if (item["source"] as? String == RecommendationSource.LOCAL.name) {
                        RecommendationSource.LOCAL
                    } else {
                        RecommendationSource.AI
                    },
                    destination = destination
                )
            }.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /** Guarda el resultado; la caché nunca debe romper el flujo principal. */
    private suspend fun saveCache(
        uid: String,
        preferences: TravelPreferences,
        recommendations: List<Recommendation>
    ) {
        try {
            val items = recommendations.map { recommendation ->
                mapOf(
                    "destinationId" to recommendation.destinationId,
                    "title" to recommendation.title,
                    "reason" to recommendation.reason,
                    "source" to recommendation.source.name
                )
            }

            firestore
                .collection("users")
                .document(uid)
                .collection(RECOMMENDATIONS_COLLECTION)
                .document(CACHE_DOCUMENT)
                .set(
                    mapOf(
                        "tripType" to preferences.tripType,
                        "budget" to preferences.budget,
                        "duration" to preferences.duration,
                        "interests" to preferences.interestNames,
                        "items" to items,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
        } catch (_: Exception) {
            // Best-effort: sin caché la IA simplemente se re-evalúa.
        }
    }

    /** Perfil estructurado + nombres de las categorías de interés marcadas. */
    suspend fun loadTravelPreferences(uid: String): TravelPreferences {        val profile = profileRepository.getProfile(uid)

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

        // users/{uid}/recommendations/last → último resultado de la IA.
        const val RECOMMENDATIONS_COLLECTION = "recommendations"
        const val CACHE_DOCUMENT = "last"

        // Vigencia de la caché: 6 horas (evita repetir llamadas a Gemini).
        const val CACHE_TTL_MILLIS: Long = 6 * 60 * 60 * 1000L
    }
}
