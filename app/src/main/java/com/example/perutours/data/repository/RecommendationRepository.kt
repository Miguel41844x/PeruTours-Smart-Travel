package com.example.perutours.data.repository

import com.example.perutours.BuildConfig
import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.model.RecommendationSource
import com.example.perutours.data.model.TravelPreferences
import com.example.perutours.data.remote.DestinationDto
import com.example.perutours.data.remote.gemini.GeminiClient
import com.example.perutours.data.remote.gemini.GeminiApiService
import com.example.perutours.data.remote.wikipedia.WikipediaApiService
import com.example.perutours.domain.recommendation.LocalRecommender
import com.example.perutours.domain.recommendation.RecommendationPromptBuilder
import com.example.perutours.domain.recommendation.RecommendationResponseParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    private val geminiApi: GeminiApiService,
    private val wikipediaApi: WikipediaApiService
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
                    return@withContext Result.success(enrichWithWikipedia(cached))
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

            // 4. Información turística externa: resumen de Wikipedia
            //    (Retrofit + corrutinas, mejor esfuerzo por destino).
            val enriched = enrichWithWikipedia(recommendations)

            // 5. Caché best-effort del resultado para próximas aperturas
            saveCache(uid, preferences, enriched)

            Result.success(enriched)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===================== Caché en Firestore (HU10) =====================

    /** Último resultado guardado si el perfil no cambió y sigue vigente. */
    private suspend fun readCache(
        uid: String,
        preferences: TravelPreferences,
        candidates: List<DestinationDto>
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

            restoreFromCacheItems(items, candidates)
                .take(MAX_RECOMMENDATIONS)
                .takeIf { it.isNotEmpty() }
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
            val items = buildCacheItems(recommendations)

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

    /**
     * HU10: consume la API externa de Wikipedia (Retrofit + corrutinas)
     * para añadir un resumen turístico a cada recomendación.
     *
     * Es mejor esfuerzo: si un destino no tiene página o falla la red,
     * la recomendación conserva su resumen vacío sin romper el flujo.
     */
    private suspend fun enrichWithWikipedia(
        recommendations: List<Recommendation>
    ): List<Recommendation> = coroutineScope {
        recommendations.map { recommendation ->
            async {
                val summary = try {
                    withTimeoutOrNull(WIKIPEDIA_TIMEOUT_MILLIS) {
                        val title = recommendation.title
                            .replace(Regex("[\\\\/:*?\"<>|]"), " ")
                            .trim()
                            .ifBlank { recommendation.title }
                        wikipediaApi.summary(title).extract
                    }
                } catch (_: Exception) {
                    null
                }

                if (summary.isNullOrBlank()) {
                    recommendation
                } else {
                    recommendation.copy(summary = summary.take(280))
                }
            }
        }.awaitAll()
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
        // Máximo de tarjetas en "Recomendado para ti".
        // Debe coincidir con el prompt de Gemini (RecommendationPromptBuilder)
        // y con el default de LocalRecommender.recommend().
        const val MAX_RECOMMENDATIONS = 3

        // users/{uid}/recommendations/last → último resultado de la IA.
        const val RECOMMENDATIONS_COLLECTION = "recommendations"
        const val CACHE_DOCUMENT = "last"

        // Vigencia de la caché: 6 horas (evita repetir llamadas a Gemini).
        const val CACHE_TTL_MILLIS: Long = 6 * 60 * 60 * 1000L

        // Timeout de cada resumen de Wikipedia (mejor esfuerzo).
        const val WIKIPEDIA_TIMEOUT_MILLIS: Long = 4_000L

        /**
         * Serializa las recomendaciones para guardarlas en Firestore.
         *
         * Incluye el paquete exacto mostrado (packageId): si un destino
         * tiene varios paquetes publicados, al volver a abrir la app se
         * restaura ESE paquete con su imagen y precio originales.
         */
        internal fun buildCacheItems(
            recommendations: List<Recommendation>
        ): List<Map<String, Any>> = recommendations.map { recommendation ->
            mapOf(
                "destinationId" to recommendation.destinationId,
                "packageId" to (recommendation.destination?.id ?: ""),
                "title" to recommendation.title,
                "reason" to recommendation.reason,
                "source" to recommendation.source.name
            )
        }

        /**
         * Reconstruye las recomendaciones desde los items de la caché.
         *
         * - Si el item registra packageId se busca ESE paquete exacto; si ya
         *   no existe en el catálogo se descarta el item (mejor recomputar
         *   que mostrar título de un paquete y precio de otro).
         * - Los items antiguos (sin packageId) usan el título guardado como
         *   pistazo para elegir el paquete correcto del destino.
         * - Los destinos repetidos de cachés antiguas se colapsan a una
         *   sola tarjeta.
         */
        internal fun restoreFromCacheItems(
            items: List<Map<String, Any?>>,
            candidates: List<DestinationDto>
        ): List<Recommendation> {
            val byPackageId = candidates.associateBy { it.id }

            return items.mapNotNull { item ->
                val destinationId = item["destinationId"] as? String
                    ?: return@mapNotNull null
                val packageId = (item["packageId"] as? String)
                    ?.takeIf { it.isNotBlank() }

                val destination = if (packageId != null) {
                    // Paquete exacto registrado al guardar; si ya no existe
                    // en el catálogo se descarta el item (mejor recomputar
                    // que mezclar el título de un paquete con el precio de otro).
                    byPackageId[packageId] ?: return@mapNotNull null
                } else {
                    // Caché antigua sin packageId: usa el título guardado
                    // como pistazo para quedarse con el paquete correcto.
                    val cachedTitle = (item["title"] as? String)
                        ?.takeIf { it.isNotBlank() }
                    val sameDestination = candidates.filter { candidate ->
                        candidate.destinationId.ifBlank { candidate.id } == destinationId
                    }
                    sameDestination.firstOrNull { it.title == cachedTitle }
                        ?: sameDestination.firstOrNull()
                        ?: return@mapNotNull null
                }

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
            }.distinctBy { it.destinationId }
        }
    }
}
