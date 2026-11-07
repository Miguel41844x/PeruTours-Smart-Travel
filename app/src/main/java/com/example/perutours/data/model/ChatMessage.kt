package com.example.perutours.data.model

import java.util.UUID

/** Quién escribió el mensaje dentro del chatbot (HU11). */
enum class ChatRole { USER, ASSISTANT }

/**
 * Resumen de una cotización REAL del cliente que se muestra como tarjeta
 * dentro del chat. Lo arma la app a partir de Firestore, nunca la IA, para
 * que el precio y el estado mostrados no puedan ser inventados.
 */
data class ChatQuotationCard(
    val quotationId: String,
    val destination: String,
    val status: String,
    val totalAmount: Double,
    val currency: String,
    val travelerCount: Int,
    val validityDate: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    /** Mensaje de error (sin conexión, etc.): se muestra con opción de reintentar. */
    val isError: Boolean = false,
    /** Respuesta armada solo con datos locales porque la IA no estuvo disponible. */
    val isFallback: Boolean = false,
    val quotationCards: List<ChatQuotationCard> = emptyList()
)