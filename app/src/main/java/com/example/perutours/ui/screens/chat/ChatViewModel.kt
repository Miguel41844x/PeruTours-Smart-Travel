package com.example.perutours.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.ChatMessage
import com.example.perutours.data.model.ChatRole
import com.example.perutours.data.repository.ChatAssistant
import com.example.perutours.data.repository.ChatException
import com.example.perutours.data.repository.ChatFailure
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * HU11: ViewModel del asistente virtual (MVVM).
 *
 * Mantiene la conversación en memoria (sobrevive a rotaciones), valida los
 * mensajes y delega la IA y el acceso a datos en [ChatAssistant].
 */
class ChatViewModel(
    private val assistant: ChatAssistant
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var replyJob: Job? = null

    init {
        viewModelScope.launch {
            val name = assistant.loadUserName()
            _uiState.update { it.copy(userName = name) }
        }
    }

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = ChatMessageValidator.limit(value)) }
    }

    /** Envía el texto escrito en el campo de entrada. */
    fun sendInput() = sendMessage(_uiState.value.input)

    /** Envía [text] (campo de texto, sugerencias o chips). Ignora mensajes vacíos. */
    fun sendMessage(text: String) {
        val state = _uiState.value
        val clean = ChatMessageValidator.normalize(text)
        if (state.isLoading || !ChatMessageValidator.isSendable(clean)) return

        val history = state.messages
        val userMessage = ChatMessage(role = ChatRole.USER, text = clean)
        _uiState.update {
            it.copy(messages = it.messages + userMessage, input = "", isLoading = true)
        }
        requestReply(history, clean)
    }

    /** Reintenta la última pregunta cuando la respuesta terminó en error. */
    fun retry() {
        val state = _uiState.value
        if (state.isLoading || state.messages.lastOrNull()?.isError != true) return

        val withoutError = state.messages.dropLast(1)
        val lastUser = withoutError.lastOrNull()?.takeIf { it.role == ChatRole.USER } ?: return

        _uiState.update { it.copy(messages = withoutError, isLoading = true) }
        requestReply(withoutError.dropLast(1), lastUser.text)
    }

    fun clearConversation() {
        replyJob?.cancel()
        _uiState.update { it.copy(messages = emptyList(), input = "", isLoading = false) }
    }

    private fun requestReply(history: List<ChatMessage>, userText: String) {
        replyJob = viewModelScope.launch {
            val result = assistant.sendMessage(history, userText)
            val reply = result.fold(
                onSuccess = { answer ->
                    ChatMessage(
                        role = ChatRole.ASSISTANT,
                        text = answer.text,
                        isFallback = answer.isFallback,
                        quotationCards = answer.quotationCards
                    )
                },
                onFailure = { error ->
                    ChatMessage(
                        role = ChatRole.ASSISTANT,
                        text = errorMessage(error),
                        isError = true
                    )
                }
            )
            _uiState.update { it.copy(messages = it.messages + reply, isLoading = false) }
        }
    }

    private fun errorMessage(error: Throwable): String =
        when ((error as? ChatException)?.failure) {
            ChatFailure.NOT_SIGNED_IN ->
                "Inicia sesión para consultar tus solicitudes y cotizaciones."
            ChatFailure.DATA_UNAVAILABLE ->
                "No pude consultar tus datos. Revisa tu conexión e inténtalo de nuevo."
            else ->
                "No pude responder en este momento. Inténtalo de nuevo."
        }
}