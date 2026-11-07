package com.example.perutours.data.repository

import com.example.perutours.BuildConfig
import com.example.perutours.data.model.ChatMessage
import com.example.perutours.data.model.ChatQuotationCard
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.remote.gemini.GeminiApiService
import com.example.perutours.data.remote.gemini.GeminiClient
import com.example.perutours.domain.chat.ChatCatalogItem
import com.example.perutours.domain.chat.ChatContextBuilder
import com.example.perutours.domain.chat.ChatFallbackResponder
import com.example.perutours.domain.chat.ChatPromptBuilder
import com.example.perutours.domain.chat.ChatQuotationCardsBuilder
import com.example.perutours.domain.chat.ChatUserData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Causas por las que el chatbot no pudo responder con la IA (HU11). */
enum class ChatFailure(val isRetryable: Boolean = false) {
    NOT_SIGNED_IN,
    DATA_UNAVAILABLE,
    NO_NETWORK,
    TIMEOUT,
    RATE_LIMITED(isRetryable = true),
    SERVICE_UNAVAILABLE(isRetryable = true),
    NOT_CONFIGURED,
    EMPTY_RESPONSE,
    UNKNOWN
}

class ChatException(val failure: ChatFailure, cause: Throwable? = null) :
    Exception(failure.name, cause)

/**
 * Respuesta del asistente. Si [isFallback] es true, la IA no estuvo disponible
 * y el texto se armó solo con los datos del cliente; [failure] indica el motivo.
 */
data class ChatReply(
    val text: String,
    val quotationCards: List<ChatQuotationCard> = emptyList(),
    val isFallback: Boolean = false,
    val failure: ChatFailure? = null
)

/** Contrato del asistente (permite probar el ViewModel con un doble). */
interface ChatAssistant {
    suspend fun loadUserName(): String
    suspend fun sendMessage(history: List<ChatMessage>, userText: String): Result<ChatReply>
}

/** Origen de los datos del cliente autenticado. */
interface ChatUserDataSource {
    suspend fun loadUserName(): String
    suspend fun loadUserData(): ChatUserData
}

/**
 * HU11: lee de Firestore SOLO lo que pertenece al usuario autenticado
 * (solicitudes por userId; cotizaciones y reservas por clientId) y el
 * catálogo local de paquetes (Room).
 */
class FirestoreChatUserDataSource(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val profileRepository: ProfileRepository,
    private val destinationRepository: DestinationRepository
) : ChatUserDataSource {

    override suspend fun loadUserName(): String {
        val uid = auth.currentUser?.uid.orEmpty()
        val fullName = runCatching { profileRepository.getProfile(uid).name }
            .getOrNull()
            .orEmpty()
            .ifBlank { auth.currentUser?.displayName.orEmpty() }
        return fullName.trim().substringBefore(' ')
    }

    override suspend fun loadUserData(): ChatUserData = coroutineScope {
        val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión para usar el asistente.")

        val requests = async {
            firestore.collection(COLLECTION_REQUESTS)
                .whereEqualTo("userId", uid)
                .get().await()
                .toObjects(TravelRequest::class.java)
        }
        val quotations = async {
            firestore.collection(COLLECTION_QUOTATIONS)
                .whereEqualTo("clientId", uid)
                .get().await()
                .toObjects(Quotation::class.java)
        }
        val reservations = async {
            firestore.collection(COLLECTION_RESERVATIONS)
                .whereEqualTo("clientId", uid)
                .get().await()
                .toObjects(Reservation::class.java)
        }
        val catalog = async { loadCatalog() }

        ChatUserData(
            userName = loadUserName(),
            requests = requests.await(),
            quotations = quotations.await(),
            reservations = reservations.await(),
            catalog = catalog.await()
        )
    }

    /** Catálogo de paquetes: mejor esfuerzo, nunca bloquea el chat. */
    private suspend fun loadCatalog(): List<ChatCatalogItem> = try {
        destinationRepository.refreshDestinations(forceRefresh = false)
        destinationRepository.localDestinations.first().map { entity ->
            val dto = entity.toDto()
            ChatCatalogItem(
                title = dto.title,
                location = dto.location,
                duration = dto.duration,
                price = dto.price,
                category = dto.category
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val COLLECTION_REQUESTS = "travel_requests"
        const val COLLECTION_QUOTATIONS = "cotizaciones"
        const val COLLECTION_RESERVATIONS = "reservations"
    }
}

/**
 * HU11: asistente virtual.
 *
 * Flujo (todo con corrutinas en Dispatchers.IO):
 *  1. Verifica la sesión y lee los datos del cliente autenticado.
 *  2. Construye el prompt (reglas + datos + historial) y llama a Gemini
 *     reutilizando el cliente Retrofit de la HU10.
 *  3. Si la IA falla (sin red, saturada, sin clave...) responde con un
 *     resumen real de los datos del cliente y explica el motivo.
 *
 * Solo falla (Result.failure) si no hay sesión o no se pueden leer los datos.
 */
class ChatRepository(
    private val currentUid: () -> String?,
    private val dataSource: ChatUserDataSource,
    private val geminiApi: GeminiApiService,
    private val apiKey: () -> String = { BuildConfig.GEMINI_API_KEY },
    private val model: String = GeminiClient.MODEL,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val retryDelayMillis: Long = RETRY_DELAY_MILLIS
) : ChatAssistant {

    override suspend fun loadUserName(): String = withContext(Dispatchers.IO) {
        try {
            dataSource.loadUserName()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            ""
        }
    }

    override suspend fun sendMessage(
        history: List<ChatMessage>,
        userText: String
    ): Result<ChatReply> = withContext(Dispatchers.IO) {
        val uid = currentUid()?.takeIf { it.isNotBlank() }
            ?: return@withContext Result.failure(ChatException(ChatFailure.NOT_SIGNED_IN))

        // Doble barrera de privacidad: aunque la consulta ya filtra por uid,
        // se descarta cualquier documento ajeno antes de armar el contexto.
        val data = try {
            dataSource.loadUserData().restrictedTo(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return@withContext Result.failure(ChatException(ChatFailure.DATA_UNAVAILABLE, e))
        }

        val now = clock()
        val cards = ChatQuotationCardsBuilder.build(userText, data.quotations)

        when (val outcome = askGemini(history, userText, data, now)) {
            is GeminiOutcome.Success -> Result.success(
                ChatReply(text = outcome.text, quotationCards = cards)
            )
            is GeminiOutcome.Failed -> {
                val summary = ChatFallbackResponder.respond(userText, data)
                Result.success(
                    ChatReply(
                        text = "${notice(outcome.failure)}\n\n" +
                                "Mientras tanto, esto es lo que tengo registrado de tu cuenta:\n\n$summary",
                        quotationCards = cards,
                        isFallback = true,
                        failure = outcome.failure
                    )
                )
            }
        }
    }

    private suspend fun askGemini(
        history: List<ChatMessage>,
        userText: String,
        data: ChatUserData,
        nowMillis: Long
    ): GeminiOutcome {
        val key = apiKey()
        if (key.isBlank()) return GeminiOutcome.Failed(ChatFailure.NOT_CONFIGURED)

        val request = ChatPromptBuilder.toRequest(
            history = history,
            userText = userText,
            contextText = ChatContextBuilder.build(data, nowMillis)
        )

        var retries = 0
        while (true) {
            try {
                val text = geminiApi
                    .generateContent(model = model, apiKey = key, request = request)
                    .text()
                    .trim()
                return if (text.isBlank()) {
                    GeminiOutcome.Failed(ChatFailure.EMPTY_RESPONSE)
                } else {
                    GeminiOutcome.Success(text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val failure = mapFailure(e)
                if (failure.isRetryable && retries < MAX_RETRIES) {
                    retries++
                    delay(retryDelayMillis)
                    continue
                }
                return GeminiOutcome.Failed(failure)
            }
        }
    }

    private sealed class GeminiOutcome {
        data class Success(val text: String) : GeminiOutcome()
        data class Failed(val failure: ChatFailure) : GeminiOutcome()
    }

    companion object {
        const val MAX_RETRIES = 1
        const val RETRY_DELAY_MILLIS = 1_500L

        /** Traduce la excepción técnica a una causa entendible por el usuario. */
        fun mapFailure(error: Throwable): ChatFailure = when (error) {
            is SocketTimeoutException -> ChatFailure.TIMEOUT
            is IOException -> ChatFailure.NO_NETWORK
            is HttpException -> when (error.code()) {
                429 -> ChatFailure.RATE_LIMITED
                401, 403 -> ChatFailure.NOT_CONFIGURED
                in 500..599 -> ChatFailure.SERVICE_UNAVAILABLE
                else -> ChatFailure.UNKNOWN
            }
            else -> ChatFailure.UNKNOWN
        }

        fun notice(failure: ChatFailure): String = when (failure) {
            ChatFailure.NO_NETWORK ->
                "No hay conexión a internet, así que no pude consultar al asistente inteligente."
            ChatFailure.TIMEOUT ->
                "El asistente inteligente tardó demasiado en responder."
            ChatFailure.RATE_LIMITED, ChatFailure.SERVICE_UNAVAILABLE ->
                "El asistente inteligente está saturado en este momento."
            ChatFailure.NOT_CONFIGURED ->
                "El asistente inteligente no está disponible en esta versión de la app."
            else ->
                "No pude obtener una respuesta del asistente inteligente."
        }
    }
}