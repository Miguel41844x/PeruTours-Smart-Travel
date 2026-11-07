package com.example.perutours.ui.screens.chat

import com.example.perutours.data.model.ChatMessage

/**
 * HU11: estado de la pantalla del asistente virtual (MVVM).
 *
 * El mensaje de bienvenida no forma parte de [messages]: la pantalla lo
 * dibuja aparte, así nunca se envía a la IA como parte del historial.
 */
data class ChatUiState(
    val userName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    /** true mientras la IA genera una respuesta (indicador de escritura). */
    val isLoading: Boolean = false
) {
    val canSend: Boolean
        get() = ChatMessageValidator.isSendable(input) && !isLoading

    /** Sugerencias de preguntas frecuentes: solo antes de la primera pregunta. */
    val showSuggestions: Boolean
        get() = messages.isEmpty()
}