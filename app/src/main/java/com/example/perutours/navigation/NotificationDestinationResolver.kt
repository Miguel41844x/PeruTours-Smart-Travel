package com.example.perutours.navigation

/** Solo reconoce tipos e identificadores propios de la app; no ejecuta rutas del payload. */
object NotificationDestinationResolver {
    fun resolve(type: String?, quotationId: String?, reservationId: String?): String? {
        fun valid(id: String?) = id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,128}")) }
        return when (type ?: "quotation") {
            "quotation" -> valid(quotationId)?.let { "customer_quotation/" + it }
            "quotation_response" -> valid(quotationId)?.let { "agent_quotation_detail/" + it }
            "payment_review" -> valid(reservationId)?.let { "customer_reservation/" + it }
            "trip_incident" -> valid(reservationId)?.let { "agent_trip/" + it }
            "trip_incident_response", "trip_reminder" -> valid(reservationId)?.let { "trip/" + it }
            else -> null
        }
    }
}
