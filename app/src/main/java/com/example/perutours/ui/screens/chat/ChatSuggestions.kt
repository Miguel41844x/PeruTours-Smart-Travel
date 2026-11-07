package com.example.perutours.ui.screens.chat

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.ui.graphics.vector.ImageVector

/** Pregunta sugerida: [label] es lo que se ve y [prompt] lo que se envía al asistente. */
data class ChatSuggestion(
    val label: String,
    val prompt: String,
    val icon: ImageVector
)

object ChatSuggestions {

    /** Tarjeta "Preguntas frecuentes" del mensaje de bienvenida. */
    val frequentQuestions = listOf(
        ChatSuggestion(
            label = "¿Cuál es el estado de mi solicitud?",
            prompt = "¿Cuál es el estado de mis solicitudes de viaje?",
            icon = Icons.AutoMirrored.Filled.Assignment
        ),
        ChatSuggestion(
            label = "¿Qué cotizaciones tengo disponibles?",
            prompt = "¿Qué cotizaciones tengo disponibles y cuál es su estado?",
            icon = Icons.AutoMirrored.Filled.ReceiptLong
        ),
        ChatSuggestion(
            label = "¿Qué paquetes turísticos tienen?",
            prompt = "¿Qué paquetes turísticos ofrece PeruTours?",
            icon = Icons.Default.Luggage
        ),
        ChatSuggestion(
            label = "¿Qué lugares puedo visitar en Cusco?",
            prompt = "¿Qué lugares puedo visitar en Cusco?",
            icon = Icons.Default.LocationOn
        )
    )

    /** Chips rápidos sobre el campo de texto. */
    val quickChips = listOf(
        ChatSuggestion(
            label = "Mis cotizaciones",
            prompt = "Muéstrame mis cotizaciones y su estado.",
            icon = Icons.AutoMirrored.Filled.ReceiptLong
        ),
        ChatSuggestion(
            label = "Mis solicitudes",
            prompt = "¿Cómo van mis solicitudes de viaje?",
            icon = Icons.AutoMirrored.Filled.Assignment
        ),
        ChatSuggestion(
            label = "Mis reservas",
            prompt = "¿Cómo van mis reservas y mis pagos?",
            icon = Icons.Default.Luggage
        ),
        ChatSuggestion(
            label = "Destinos turísticos",
            prompt = "¿Qué destinos turísticos del Perú me recomiendas visitar?",
            icon = Icons.Default.LocationOn
        )
    )
}