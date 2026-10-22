package com.example.perutours.data.model

import java.util.concurrent.TimeUnit

/**
 * Actividad confirmada o pendiente de programación dentro de una reserva pagada.
 * Los valores String se mantienen deliberadamente para que Firestore pueda
 * reconstruir el objeto sin convertidores personalizados.
 */
data class TripItineraryItem(
    val id: String = "",
    val reservationId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = CATEGORY_ACTIVITY,
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val startAtMillis: Long = 0L,
    val endAtMillis: Long = 0L,
    val dayIndex: Int = 1,
    val sortOrder: Int = 0,
    val providerName: String = "",
    val status: String = STATUS_SCHEDULED,
    val notes: String = "",
    val source: String = SOURCE_AGENT,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val hasCoordinates: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            (latitude != 0.0 || longitude != 0.0)

    companion object {
        const val CATEGORY_ACTIVITY = "Actividad"
        const val CATEGORY_TRANSPORT = "Transporte"
        const val CATEGORY_LODGING = "Alojamiento"
        const val CATEGORY_FOOD = "Alimentación"
        const val CATEGORY_OTHER = "Otro"

        val VALID_CATEGORIES = setOf(
            CATEGORY_ACTIVITY,
            CATEGORY_TRANSPORT,
            CATEGORY_LODGING,
            CATEGORY_FOOD,
            CATEGORY_OTHER
        )

        const val STATUS_SCHEDULED = "Programado"
        const val STATUS_COMPLETED = "Completado"
        const val STATUS_CANCELLED = "Cancelado"
        const val STATUS_PENDING_SCHEDULE = "Horario por confirmar"

        val VALID_STATUSES = setOf(
            STATUS_SCHEDULED,
            STATUS_COMPLETED,
            STATUS_CANCELLED,
            STATUS_PENDING_SCHEDULE
        )

        const val SOURCE_AGENT = "agent"
        const val SOURCE_RESERVATION = "reservation"
    }
}

/** Elemento de preparación personal que pertenece únicamente al viajero. */
data class TripChecklistItem(
    val id: String = "",
    val reservationId: String = "",
    val clientId: String = "",
    val title: String = "",
    val category: String = CATEGORY_LUGGAGE,
    val isCompleted: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    companion object {
        const val CATEGORY_LUGGAGE = "Equipaje"
        const val CATEGORY_DOCUMENTS = "Documentos"
        const val CATEGORY_HEALTH = "Salud"
        const val CATEGORY_MONEY = "Dinero"
        const val CATEGORY_OTHER = "Otros"

        val VALID_CATEGORIES = setOf(
            CATEGORY_LUGGAGE,
            CATEGORY_DOCUMENTS,
            CATEGORY_HEALTH,
            CATEGORY_MONEY,
            CATEGORY_OTHER
        )
    }
}

/** Incidencia que permite solicitar ayuda al agente durante el viaje. */
data class TripIncident(
    val id: String = "",
    val reservationId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val category: String = CATEGORY_OTHER,
    val priority: String = PRIORITY_MEDIUM,
    val status: String = STATUS_OPEN,
    val description: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val agentResponse: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val resolvedAtMillis: Long? = null
) {
    val hasCoordinates: Boolean
        get() = latitude in -90.0..90.0 &&
            longitude in -180.0..180.0 &&
            (latitude != 0.0 || longitude != 0.0)

    companion object {
        const val CATEGORY_TRANSPORT = "Transporte"
        const val CATEGORY_LODGING = "Alojamiento"
        const val CATEGORY_ACTIVITY = "Actividad"
        const val CATEGORY_HEALTH = "Salud"
        const val CATEGORY_OTHER = "Otro"

        val VALID_CATEGORIES = setOf(
            CATEGORY_TRANSPORT,
            CATEGORY_LODGING,
            CATEGORY_ACTIVITY,
            CATEGORY_HEALTH,
            CATEGORY_OTHER
        )

        const val PRIORITY_LOW = "Baja"
        const val PRIORITY_MEDIUM = "Media"
        const val PRIORITY_HIGH = "Alta"
        const val PRIORITY_URGENT = "Urgente"

        val VALID_PRIORITIES = setOf(
            PRIORITY_LOW,
            PRIORITY_MEDIUM,
            PRIORITY_HIGH,
            PRIORITY_URGENT
        )

        const val STATUS_OPEN = "Abierta"
        const val STATUS_IN_REVIEW = "En revisión"
        const val STATUS_RESOLVED = "Resuelta"
        const val STATUS_CLOSED = "Cerrada"

        val VALID_STATUSES = setOf(
            STATUS_OPEN,
            STATUS_IN_REVIEW,
            STATUS_RESOLVED,
            STATUS_CLOSED
        )
    }
}

/** Información que consume la pantalla principal de un viaje. */
data class TripOverview(
    val reservation: Reservation,
    val itinerary: List<TripItineraryItem> = emptyList(),
    val checklist: List<TripChecklistItem> = emptyList(),
    val incidents: List<TripIncident> = emptyList(),
    val isOffline: Boolean = false
) {
    val completedChecklistCount: Int
        get() = checklist.count(TripChecklistItem::isCompleted)

    val checklistProgress: Float
        get() = if (checklist.isEmpty()) 0f else completedChecklistCount.toFloat() / checklist.size

    val openIncidentCount: Int
        get() = incidents.count {
            it.status == TripIncident.STATUS_OPEN || it.status == TripIncident.STATUS_IN_REVIEW
        }
}

/** Validaciones de dominio compartidas por repositorios, ViewModels y pruebas. */
object TripDomainRules {
    private const val MAX_CHECKLIST_TITLE = 100
    private const val MAX_INCIDENT_DESCRIPTION = 1_000
    private const val MAX_AGENT_RESPONSE = 1_000
    private const val MAX_ITINERARY_TITLE = 120
    private const val MAX_ITINERARY_DESCRIPTION = 1_500

    fun isTripAvailable(reservation: Reservation): Boolean =
        reservation.status == Reservation.STATUS_PAID

    fun tripPhase(reservation: Reservation, nowMillis: Long = System.currentTimeMillis()): String = when {
        reservation.returnAtMillis in 1 until nowMillis -> PHASE_FINISHED
        reservation.departureAtMillis > nowMillis -> PHASE_UPCOMING
        reservation.departureAtMillis in 1..nowMillis && reservation.returnAtMillis >= nowMillis -> {
            PHASE_IN_PROGRESS
        }
        else -> PHASE_UNSCHEDULED
    }

    fun tripDurationDays(reservation: Reservation): Int {
        if (reservation.departureAtMillis <= 0L || reservation.returnAtMillis <= 0L) return 1
        val elapsed = (reservation.returnAtMillis - reservation.departureAtMillis).coerceAtLeast(0L)
        return (TimeUnit.MILLISECONDS.toDays(elapsed) + 1L).coerceAtMost(365L).toInt()
    }

    fun validateChecklistTitle(title: String): String? = when {
        title.isBlank() -> "Escribe el elemento que deseas recordar."
        title.trim().length > MAX_CHECKLIST_TITLE -> {
            "El elemento no puede superar $MAX_CHECKLIST_TITLE caracteres."
        }
        else -> null
    }

    fun validateIncident(description: String, category: String, priority: String): String? = when {
        category !in TripIncident.VALID_CATEGORIES -> "Selecciona una categoría válida."
        priority !in TripIncident.VALID_PRIORITIES -> "Selecciona una prioridad válida."
        description.trim().length < 10 -> "Describe el problema con al menos 10 caracteres."
        description.trim().length > MAX_INCIDENT_DESCRIPTION -> {
            "La descripción no puede superar $MAX_INCIDENT_DESCRIPTION caracteres."
        }
        else -> null
    }

    fun validateAgentResponse(response: String): String? = when {
        response.trim().length < 5 -> "La respuesta debe tener al menos 5 caracteres."
        response.trim().length > MAX_AGENT_RESPONSE -> {
            "La respuesta no puede superar $MAX_AGENT_RESPONSE caracteres."
        }
        else -> null
    }

    fun validateItineraryItem(item: TripItineraryItem): String? = when {
        item.reservationId.isBlank() -> "La actividad debe pertenecer a una reserva."
        item.title.isBlank() -> "Escribe el nombre de la actividad."
        item.title.trim().length > MAX_ITINERARY_TITLE -> {
            "El título no puede superar $MAX_ITINERARY_TITLE caracteres."
        }
        item.description.length > MAX_ITINERARY_DESCRIPTION -> {
            "La descripción no puede superar $MAX_ITINERARY_DESCRIPTION caracteres."
        }
        item.category !in TripItineraryItem.VALID_CATEGORIES -> "Selecciona una categoría válida."
        item.status !in TripItineraryItem.VALID_STATUSES -> "Selecciona un estado válido."
        item.dayIndex !in 1..365 -> "El día del itinerario no es válido."
        item.startAtMillis < 0L || item.endAtMillis < 0L -> "La fecha de la actividad no es válida."
        item.endAtMillis > 0L && item.startAtMillis > item.endAtMillis -> {
            "La hora de fin debe ser posterior a la hora de inicio."
        }
        else -> null
    }

    const val PHASE_UPCOMING = "Próximo"
    const val PHASE_IN_PROGRESS = "En curso"
    const val PHASE_FINISHED = "Finalizado"
    const val PHASE_UNSCHEDULED = "Sin fecha"
}

/**
 * Construye una vista provisional con los servicios ya comprados. No inventa
 * horas: el agente podrá reemplazarla con actividades confirmadas en Firestore.
 */
object ReservationItineraryBuilder {
    fun build(reservation: Reservation): List<TripItineraryItem> {
        val durationDays = TripDomainRules.tripDurationDays(reservation)
        return reservation.services.mapIndexed { index, service ->
            val dayIndex = (index % durationDays) + 1
            TripItineraryItem(
                id = "reservation-${service.id.ifBlank { index.toString() }}",
                reservationId = reservation.id,
                clientId = reservation.clientId,
                agentId = reservation.agentId,
                title = service.name,
                description = "Servicio incluido en tu reserva.",
                category = mapServiceCategory(service.category),
                dayIndex = dayIndex,
                sortOrder = index,
                providerName = service.supplierName,
                status = TripItineraryItem.STATUS_PENDING_SCHEDULE,
                source = TripItineraryItem.SOURCE_RESERVATION
            )
        }
    }

    private fun mapServiceCategory(category: String): String {
        val normalized = category.lowercase()
        return when {
            "transport" in normalized || "traslado" in normalized -> {
                TripItineraryItem.CATEGORY_TRANSPORT
            }
            "hotel" in normalized || "aloj" in normalized -> TripItineraryItem.CATEGORY_LODGING
            "aliment" in normalized || "comida" in normalized -> TripItineraryItem.CATEGORY_FOOD
            "tour" in normalized || "actividad" in normalized -> TripItineraryItem.CATEGORY_ACTIVITY
            else -> TripItineraryItem.CATEGORY_OTHER
        }
    }
}
