package com.example.perutours.domain.chat

import com.example.perutours.data.model.ChatQuotationCard
import com.example.perutours.data.model.Quotation
import java.text.Normalizer
import java.util.Locale

/** Detección sencilla (sin IA) del tema de la pregunta; ignora tildes y mayúsculas. */
object ChatIntentDetector {

    fun mentionsQuotations(text: String): Boolean = containsAny(text, "cotiz")

    fun mentionsRequests(text: String): Boolean = containsAny(text, "solicitud", "pedido")

    fun mentionsReservations(text: String): Boolean =
        containsAny(text, "reserva", "pago", "pague", "pagu", "comprobante")

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        val normalized = normalize(text)
        return keywords.any { normalized.contains(it) }
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
}

/**
 * Tarjetas de cotización que acompañan la respuesta cuando el cliente
 * pregunta por sus cotizaciones. Salen de datos reales, no de la IA.
 */
object ChatQuotationCardsBuilder {

    const val MAX_CARDS = 2

    fun build(userText: String, quotations: List<Quotation>): List<ChatQuotationCard> {
        if (!ChatIntentDetector.mentionsQuotations(userText)) return emptyList()
        return quotations
            .sortedByDescending { it.createdAtMillis }
            .take(MAX_CARDS)
            .map { quotation ->
                ChatQuotationCard(
                    quotationId = quotation.id,
                    destination = quotation.destination,
                    status = quotation.status,
                    totalAmount = quotation.totalAmount,
                    currency = quotation.currency,
                    travelerCount = quotation.travelerCount,
                    validityDate = quotation.validityDate
                )
            }
    }
}