package com.example.perutours.ui.screens.trip

import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripDomainRules
import com.example.perutours.data.model.TripItineraryItem
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

data class AgentItineraryDraft(
    val title: String = "",
    val description: String = "",
    val category: String = TripItineraryItem.CATEGORY_ACTIVITY,
    val dayText: String = "1",
    val timeText: String = "09:00",
    val durationMinutesText: String = "60",
    val locationName: String = "",
    val providerName: String = "",
    val notes: String = ""
)

object AgentItineraryForm {
    private val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

    fun validate(draft: AgentItineraryDraft, reservation: Reservation): String? {
        val day = draft.dayText.toIntOrNull()
        val duration = draft.durationMinutesText.toIntOrNull()
        val maxDays = TripDomainRules.tripDurationDays(reservation)
        return when {
            draft.title.trim().length !in 3..120 -> {
                "El título debe tener entre 3 y 120 caracteres."
            }
            draft.description.length > 1_500 -> "La descripción no puede superar 1500 caracteres."
            draft.category !in TripItineraryItem.VALID_CATEGORIES -> "Selecciona una categoría válida."
            day == null || day !in 1..maxDays -> "El día debe estar entre 1 y $maxDays."
            !timePattern.matches(draft.timeText.trim()) -> "Usa una hora válida en formato HH:mm."
            duration == null || duration !in 15..1_440 -> {
                "La duración debe estar entre 15 y 1440 minutos."
            }
            draft.locationName.length > 160 -> "La ubicación no puede superar 160 caracteres."
            draft.providerName.length > 120 -> "El proveedor no puede superar 120 caracteres."
            draft.notes.length > 500 -> "Las notas no pueden superar 500 caracteres."
            else -> null
        }
    }

    fun toModel(
        draft: AgentItineraryDraft,
        reservation: Reservation,
        existing: TripItineraryItem? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): TripItineraryItem {
        validate(draft, reservation)?.let { throw IllegalArgumentException(it) }
        val day = draft.dayText.toInt()
        val parts = draft.timeText.trim().split(":")
        val start = Calendar.getInstance().apply {
            timeInMillis = reservation.departureAtMillis
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, day - 1)
        }.timeInMillis
        val end = start + TimeUnit.MINUTES.toMillis(draft.durationMinutesText.toLong())

        return TripItineraryItem(
            id = existing?.id ?: UUID.randomUUID().toString(),
            reservationId = reservation.id,
            clientId = reservation.clientId,
            agentId = reservation.agentId,
            title = draft.title.trim(),
            description = draft.description.trim(),
            category = draft.category,
            locationName = draft.locationName.trim(),
            latitude = existing?.latitude ?: 0.0,
            longitude = existing?.longitude ?: 0.0,
            startAtMillis = start,
            endAtMillis = end,
            dayIndex = day,
            sortOrder = existing?.sortOrder
                ?: (day * 10_000 + parts[0].toInt() * 60 + parts[1].toInt()),
            providerName = draft.providerName.trim(),
            status = existing?.status ?: TripItineraryItem.STATUS_SCHEDULED,
            notes = draft.notes.trim(),
            source = TripItineraryItem.SOURCE_AGENT,
            createdAtMillis = existing?.createdAtMillis?.takeIf { it > 0L } ?: nowMillis,
            updatedAtMillis = nowMillis
        )
    }

    fun fromModel(item: TripItineraryItem): AgentItineraryDraft {
        val time = Calendar.getInstance().apply { timeInMillis = item.startAtMillis }
        val durationMinutes = if (item.endAtMillis > item.startAtMillis) {
            TimeUnit.MILLISECONDS.toMinutes(item.endAtMillis - item.startAtMillis)
        } else {
            60L
        }
        return AgentItineraryDraft(
            title = item.title,
            description = item.description,
            category = item.category,
            dayText = item.dayIndex.toString(),
            timeText = "%02d:%02d".format(
                time.get(Calendar.HOUR_OF_DAY),
                time.get(Calendar.MINUTE)
            ),
            durationMinutesText = durationMinutes.toString(),
            locationName = item.locationName,
            providerName = item.providerName,
            notes = item.notes
        )
    }
}
