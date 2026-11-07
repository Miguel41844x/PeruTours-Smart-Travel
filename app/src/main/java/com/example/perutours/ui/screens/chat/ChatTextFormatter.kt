package com.example.perutours.ui.screens.chat

/** Trozo de texto de una respuesta, con o sin negrita. */
data class ChatTextSegment(val text: String, val bold: Boolean)

/**
 * Gemini suele responder con un poco de Markdown. Esta clase lo reduce a lo
 * único que el chat sabe dibujar: **negritas** y viñetas.
 */
object ChatTextFormatter {

    fun format(raw: String): List<ChatTextSegment> {
        val normalized = raw.trim().lines().joinToString("\n") { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("* ") -> "• " + trimmed.removePrefix("* ")
                trimmed.startsWith("- ") -> "• " + trimmed.removePrefix("- ")
                trimmed.startsWith("#") -> "**" + trimmed.trimStart('#').trim() + "**"
                else -> line
            }
        }

        var parts = normalized.split("**")
        // Número impar de "**": el último no tiene cierre y se deja como texto.
        val delimiters = parts.size - 1
        if (delimiters % 2 == 1) {
            parts = parts.dropLast(2) + (parts[delimiters - 1] + "**" + parts[delimiters])
        }

        return parts.mapIndexedNotNull { index, part ->
            if (part.isEmpty()) null else ChatTextSegment(part, bold = index % 2 == 1)
        }
    }
}