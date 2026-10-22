package com.example.perutours.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TripDomainRulesTest {

    @Test
    fun `solo una reserva pagada habilita Mi viaje`() {
        assertTrue(TripDomainRules.isTripAvailable(Reservation(status = Reservation.STATUS_PAID)))
        assertFalse(
            TripDomainRules.isTripAvailable(
                Reservation(status = Reservation.STATUS_PAYMENT_PENDING)
            )
        )
    }

    @Test
    fun `calcula dias inclusivos dentro del rango`() {
        val departure = 1_700_000_000_000L
        val reservation = Reservation(
            departureAtMillis = departure,
            returnAtMillis = departure + TimeUnit.DAYS.toMillis(3)
        )

        assertEquals(4, TripDomainRules.tripDurationDays(reservation))
    }

    @Test
    fun `rechaza incidencia demasiado corta`() {
        val error = TripDomainRules.validateIncident(
            description = "Ayuda",
            category = TripIncident.CATEGORY_TRANSPORT,
            priority = TripIncident.PRIORITY_HIGH
        )

        assertEquals("Describe el problema con al menos 10 caracteres.", error)
    }

    @Test
    fun `acepta incidencia completa`() {
        val error = TripDomainRules.validateIncident(
            description = "El vehículo no llegó al punto de encuentro indicado.",
            category = TripIncident.CATEGORY_TRANSPORT,
            priority = TripIncident.PRIORITY_HIGH
        )

        assertNull(error)
    }

    @Test
    fun `construye actividades provisionales sin inventar horarios`() {
        val services = listOf(
            QuotationServiceItem(id = "hotel", name = "Hotel", category = "Alojamiento"),
            QuotationServiceItem(id = "tour", name = "City tour", category = "Tour")
        )
        val reservation = Reservation(
            id = "reservation-1",
            clientId = "client-1",
            services = services
        )

        val itinerary = ReservationItineraryBuilder.build(reservation)

        assertEquals(2, itinerary.size)
        assertTrue(itinerary.all { it.startAtMillis == 0L })
        assertTrue(itinerary.all { it.source == TripItineraryItem.SOURCE_RESERVATION })
        assertEquals(TripItineraryItem.CATEGORY_LODGING, itinerary.first().category)
    }
}
