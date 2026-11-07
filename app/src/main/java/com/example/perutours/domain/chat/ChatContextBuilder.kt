package com.example.perutours.domain.chat

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequest

/**
 * HU11: convierte los datos del cliente autenticado en el texto de contexto
 * que recibe Gemini.
 *
 * Es una función pura (fácil de probar) y a propósito compacta: solo los
 * registros más recientes y solo los campos útiles. No incluye correos,
 * teléfonos ni identificadores internos.
 */
object ChatContextBuilder {

    const val MAX_ITEMS = 5
    const val MAX_CATALOG = 12
    private const val MAX_FREE_TEXT = 220
    private const val MAX_SERVICES = 8

    fun build(
        data: ChatUserData,
        nowMillis: Long,
        formatters: ChatFormatters = ChatFormatters()
    ): String = buildString {
        appendLine("FECHA DE HOY: ${formatters.date(nowMillis)}")
        if (data.userName.isNotBlank()) {
            appendLine("NOMBRE DEL CLIENTE: ${data.userName}")
        }
        appendLine()

        appendSection(
            title = "SOLICITUDES DE VIAJE DEL CLIENTE",
            items = data.requests.sortedByDescending { it.createdAtMillis },
            emptyText = "El cliente no tiene solicitudes de viaje registradas."
        ) { index, request -> describeRequest(index, request, formatters) }

        appendSection(
            title = "COTIZACIONES DEL CLIENTE",
            items = data.quotations.sortedByDescending { it.createdAtMillis },
            emptyText = "El cliente no tiene cotizaciones registradas."
        ) { index, quotation -> describeQuotation(index, quotation, nowMillis, formatters) }

        appendSection(
            title = "RESERVAS Y PAGOS DEL CLIENTE",
            items = data.reservations.sortedByDescending { it.createdAtMillis },
            emptyText = "El cliente no tiene reservas registradas."
        ) { index, reservation -> describeReservation(index, reservation, formatters) }

        appendLine("CATÁLOGO DE PAQUETES DE PERUTOURS (referencial):")
        if (data.catalog.isEmpty()) {
            appendLine("- No hay información del catálogo disponible en este momento.")
        } else {
            data.catalog.take(MAX_CATALOG).forEach { item ->
                appendLine(
                    "- ${item.title} (${item.location}) · ${item.duration} · " +
                            "desde ${item.price} · categoría ${item.category}"
                )
            }
        }
    }.trimEnd()

    private fun <T> StringBuilder.appendSection(
        title: String,
        items: List<T>,
        emptyText: String,
        describe: (Int, T) -> String
    ) {
        if (items.isEmpty()) {
            appendLine("$title:")
            appendLine("- $emptyText")
        } else {
            val shown = items.take(MAX_ITEMS)
            appendLine("$title (${shown.size} más recientes de ${items.size}):")
            shown.forEachIndexed { index, item -> appendLine(describe(index + 1, item)) }
        }
        appendLine()
    }

    private fun describeRequest(
        index: Int,
        request: TravelRequest,
        formatters: ChatFormatters
    ): String = buildString {
        append("- Solicitud $index: destino ${request.destination}")
        if (request.originCity.isNotBlank()) append("; sale desde ${request.originCity}")
        append("; ida ${formatters.date(request.departureAtMillis)}")
        append("; regreso ${formatters.date(request.returnAtMillis)}")
        append("; ${request.travelerCount} viajero(s)")
        append("; estado «${request.status}»")
        if (request.notes.isNotBlank()) append("; notas: ${request.notes.limit()}")
    }

    private fun describeQuotation(
        index: Int,
        quotation: Quotation,
        nowMillis: Long,
        formatters: ChatFormatters
    ): String = buildString {
        append("- Cotización $index: destino ${quotation.destination}")
        append("; ${quotation.travelerCount} viajero(s)")
        append("; total ${formatters.money(quotation.totalAmount, quotation.currency)}")
        append("; estado «${quotation.status}»")
        if (quotation.validityDate.isNotBlank()) append("; vigente hasta ${quotation.validityDate}")
        if (isExpired(quotation, nowMillis)) append(" (YA VENCIDA)")
        if (quotation.services.isNotEmpty()) {
            val services = quotation.services.take(MAX_SERVICES).joinToString(", ") { service ->
                if (service.category.isBlank()) service.name else "${service.name} (${service.category})"
            }
            append("; servicios incluidos: $services")
        }
        if (quotation.conditions.isNotBlank()) append("; condiciones: ${quotation.conditions.limit()}")
        if (quotation.latestObservation.isNotBlank()) {
            append("; última observación del cliente: ${quotation.latestObservation.limit()}")
        }
    }

    private fun describeReservation(
        index: Int,
        reservation: Reservation,
        formatters: ChatFormatters
    ): String = buildString {
        append("- Reserva $index: destino ${reservation.destination}")
        append("; ida ${formatters.date(reservation.departureAtMillis)}")
        append("; ${reservation.travelerCount} viajero(s)")
        append("; total ${formatters.money(reservation.totalAmount, reservation.currency)}")
        append("; estado «${reservation.status}»")
        if (reservation.paymentMethod.isNotBlank()) append("; método de pago ${reservation.paymentMethod}")
        reservation.paidAtMillis?.let { append("; pagada el ${formatters.date(it)}") }
    }

    /** Una cotización «Cotizado» cuya vigencia ya pasó ya no se puede aceptar. */
    fun isExpired(quotation: Quotation, nowMillis: Long): Boolean =
        quotation.status == Quotation.STATUS_QUOTED &&
                quotation.validUntilMillis > 0L &&
                nowMillis > quotation.validUntilMillis

    private fun String.limit(): String {
        val clean = trim().replace('\n', ' ')
        return if (clean.length <= MAX_FREE_TEXT) clean else clean.take(MAX_FREE_TEXT) + "…"
    }
}