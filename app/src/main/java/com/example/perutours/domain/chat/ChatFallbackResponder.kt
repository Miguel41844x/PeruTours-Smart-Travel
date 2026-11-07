package com.example.perutours.domain.chat

/**
 * HU11: respuesta de respaldo cuando la IA no está disponible (sin internet,
 * servicio saturado, sin clave...). Resume los datos reales del cliente para
 * que el chat siga siendo útil, igual que el recomendador local de la HU10.
 */
object ChatFallbackResponder {

    private const val MAX_LISTED = 3

    fun respond(
        userText: String,
        data: ChatUserData,
        formatters: ChatFormatters = ChatFormatters()
    ): String {
        val wantsRequests = ChatIntentDetector.mentionsRequests(userText)
        val wantsQuotations = ChatIntentDetector.mentionsQuotations(userText)
        val wantsReservations = ChatIntentDetector.mentionsReservations(userText)
        val specific = wantsRequests || wantsQuotations || wantsReservations

        val blocks = mutableListOf<String>()
        if (!specific || wantsRequests) blocks += requestsBlock(data, formatters)
        if (!specific || wantsQuotations) blocks += quotationsBlock(data, formatters)
        if (!specific || wantsReservations) blocks += reservationsBlock(data, formatters)

        val nothingRegistered =
            data.requests.isEmpty() && data.quotations.isEmpty() && data.reservations.isEmpty()

        return buildString {
            if (!specific && nothingRegistered) {
                append("Aún no tienes solicitudes, cotizaciones ni reservas registradas. ")
                append("Puedes crear una solicitud de viaje desde la pantalla de Inicio.")
            } else {
                append(blocks.joinToString("\n\n"))
            }
            if (!specific) {
                append("\n\nPregúntame por tus solicitudes, cotizaciones o reservas y te las muestro.")
            }
        }
    }

    private fun requestsBlock(data: ChatUserData, f: ChatFormatters): String {
        if (data.requests.isEmpty()) return "No tienes solicitudes de viaje registradas."
        val lines = data.requests.sortedByDescending { it.createdAtMillis }.take(MAX_LISTED)
            .joinToString("\n") {
                "- ${it.destination} · ${it.travelerCount} viajero(s) · salida ${f.date(it.departureAtMillis)} · ${it.status}"
            }
        return "Tus solicitudes de viaje:\n$lines"
    }

    private fun quotationsBlock(data: ChatUserData, f: ChatFormatters): String {
        if (data.quotations.isEmpty()) return "Todavía no tienes cotizaciones disponibles."
        val lines = data.quotations.sortedByDescending { it.createdAtMillis }.take(MAX_LISTED)
            .joinToString("\n") {
                "- ${it.destination} · ${f.money(it.totalAmount, it.currency)} · ${it.status}"
            }
        return "Tus cotizaciones:\n$lines"
    }

    private fun reservationsBlock(data: ChatUserData, f: ChatFormatters): String {
        if (data.reservations.isEmpty()) return "No tienes reservas registradas."
        val lines = data.reservations.sortedByDescending { it.createdAtMillis }.take(MAX_LISTED)
            .joinToString("\n") {
                "- ${it.destination} · ${f.money(it.totalAmount, it.currency)} · ${it.status}"
            }
        return "Tus reservas:\n$lines"
    }
}