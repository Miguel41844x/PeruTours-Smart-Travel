package com.example.perutours.domain.chat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formato de fechas y montos usado por el chatbot (contexto, respaldo y tarjetas). */
class ChatFormatters(timeZone: TimeZone = TimeZone.getDefault()) {

    private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).apply {
        this.timeZone = timeZone
    }

    fun date(millis: Long): String =
        if (millis <= 0L) "sin fecha" else dateFormat.format(Date(millis))

    fun money(amount: Double, currency: String): String {
        val symbol = if (currency.isBlank() || currency.equals("PEN", ignoreCase = true)) {
            "S/"
        } else {
            currency.uppercase(Locale.ROOT)
        }
        // Locale.US fija "1,234.50" sin depender del idioma del dispositivo.
        return String.format(Locale.US, "%s %,.2f", symbol, amount)
    }
}