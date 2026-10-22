package com.example.perutours.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.perutours.data.local.AppDatabase
import com.example.perutours.data.model.QuotationServiceItem
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripChecklistItem
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem
import com.example.perutours.service.TripBackgroundScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TripRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var remote: FakeTripRemoteDataSource
    private lateinit var repository: TripRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        remote = FakeTripRemoteDataSource()
        repository = TripRepository(remote, database.tripDao(), FakeTripScheduler())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `refresca y conserva viajes pagados en Room`() = runBlocking {
        remote.reservations = listOf(paidReservation())

        assertTrue(repository.refreshTrips().isSuccess)

        val cached = repository.observeTrips().first()
        assertEquals(1, cached.size)
        assertEquals("Cusco", cached.first().destination)
    }

    @Test
    fun `crea vista provisional si el agente aun no programa actividades`() = runBlocking {
        val reservation = paidReservation()
        remote.reservations = listOf(reservation)

        assertTrue(repository.refreshTrip(reservation.id).isSuccess)

        val overview = repository.observeTrip(reservation.id).first { it != null }!!
        assertEquals(1, overview.itinerary.size)
        assertEquals(
            TripItineraryItem.STATUS_PENDING_SCHEDULE,
            overview.itinerary.first().status
        )
    }

    @Test
    fun `un fallo remoto no elimina la cache existente`() = runBlocking {
        remote.reservations = listOf(paidReservation())
        repository.refreshTrips()
        remote.error = IllegalStateException("sin conexión")

        assertTrue(repository.refreshTrips().isFailure)
        assertEquals(1, repository.observeTrips().first().size)
    }

    @Test
    fun `agrega checklist local antes de sincronizar`() = runBlocking {
        remote.reservations = listOf(paidReservation())
        repository.refreshTrip("reservation-1")

        val result = repository.addChecklistItem(
            reservationId = "reservation-1",
            title = "Documento de identidad",
            category = TripChecklistItem.CATEGORY_DOCUMENTS
        )

        assertTrue(result.isSuccess)
        val overview = repository.observeTrip("reservation-1").first { it != null }!!
        assertEquals("Documento de identidad", overview.checklist.single().title)
    }

    @Test
    fun `actualiza y elimina checklist de forma optimista`() = runBlocking {
        remote.reservations = listOf(paidReservation())
        repository.refreshTrip("reservation-1")
        val itemId = repository.addChecklistItem(
            reservationId = "reservation-1",
            title = "Cargar batería externa",
            category = TripChecklistItem.CATEGORY_LUGGAGE
        ).getOrThrow()

        repository.toggleChecklistItem(itemId).getOrThrow()
        var overview = repository.observeTrip("reservation-1").first { it != null }!!
        assertTrue(overview.checklist.single().isCompleted)

        repository.deleteChecklistItem(itemId).getOrThrow()
        overview = repository.observeTrip("reservation-1").first { it != null }!!
        assertTrue(overview.checklist.isEmpty())
    }

    private fun paidReservation() = Reservation(
        id = "reservation-1",
        quotationId = "quotation-1",
        clientId = "client-1",
        agentId = "agent-1",
        destination = "Cusco",
        originCity = "Lima",
        departureAtMillis = 1_800_000_000_000L,
        returnAtMillis = 1_800_086_400_000L,
        services = listOf(
            QuotationServiceItem(
                id = "service-1",
                name = "City tour",
                category = "Actividad"
            )
        ),
        totalAmount = 1_500.0,
        status = Reservation.STATUS_PAID
    )

    private class FakeTripRemoteDataSource : TripRemoteDataSource {
        var reservations: List<Reservation> = emptyList()
        var itinerary: List<TripItineraryItem> = emptyList()
        var checklist: List<TripChecklistItem> = emptyList()
        var incidents: List<TripIncident> = emptyList()
        var error: Throwable? = null

        override fun currentUserId(): String = "client-1"

        override suspend fun getPaidReservations(): List<Reservation> {
            error?.let { throw it }
            return reservations
        }

        override suspend fun getPaidReservation(reservationId: String): Reservation? {
            error?.let { throw it }
            return reservations.firstOrNull { it.id == reservationId }
        }

        override suspend fun getItinerary(reservationId: String) = itinerary

        override suspend fun getChecklist(reservationId: String) = checklist

        override suspend fun getIncidents(reservationId: String) = incidents
    }

    private class FakeTripScheduler : TripBackgroundScheduler {
        override fun enqueueChecklistSync() = Unit
        override fun scheduleDepartureReminder(reservation: Reservation) = Unit
    }
}
