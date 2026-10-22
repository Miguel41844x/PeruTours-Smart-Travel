package com.example.perutours.ui.screens.trip

import com.example.perutours.data.model.Reservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AgentItineraryFormTest {

    private val departure = 1_800_000_000_000L
    private val reservation = Reservation(
        id = "reservation-1",
        clientId = "client-1",
        agentId = "agent-1",
        departureAtMillis = departure,
        returnAtMillis = departure + TimeUnit.DAYS.toMillis(3)
    )

    @Test
    fun `valida dia y hora dentro del viaje`() {
        assertNull(
            AgentItineraryForm.validate(
                AgentItineraryDraft(title = "Visita guiada", dayText = "2", timeText = "14:30"),
                reservation
            )
        )
    }

    @Test
    fun `rechaza actividad fuera del rango de viaje`() {
        val error = AgentItineraryForm.validate(
            AgentItineraryDraft(title = "Visita guiada", dayText = "8"),
            reservation
        )

        assertTrue(error.orEmpty().contains("entre 1 y 4"))
    }

    @Test
    fun `convierte borrador en actividad con participantes de la reserva`() {
        val item = AgentItineraryForm.toModel(
            draft = AgentItineraryDraft(
                title = "Museo Inka",
                dayText = "1",
                timeText = "10:15",
                durationMinutesText = "90"
            ),
            reservation = reservation,
            nowMillis = 42L
        )

        assertEquals("client-1", item.clientId)
        assertEquals("agent-1", item.agentId)
        assertEquals(TimeUnit.MINUTES.toMillis(90), item.endAtMillis - item.startAtMillis)
    }
}
