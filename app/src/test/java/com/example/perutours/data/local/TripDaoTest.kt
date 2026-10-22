package com.example.perutours.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
class TripDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TripDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.tripDao()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun `reemplaza viajes del cliente sin afectar otro usuario`() = runBlocking {
        dao.upsertTrips(
            listOf(
                trip("old", "client-1"),
                trip("other", "client-2")
            )
        )

        dao.replaceTrips("client-1", listOf(trip("new", "client-1")))

        assertEquals(listOf("new"), dao.observeTrips("client-1").first().map { it.reservationId })
        assertEquals(listOf("other"), dao.observeTrips("client-2").first().map { it.reservationId })
    }

    @Test
    fun `mantiene cambios pendientes al mezclar checklist remoto`() = runBlocking {
        val pending = checklist("pending", TripChecklistEntity.PENDING_UPSERT)
        val synced = checklist("old-synced", TripChecklistEntity.SYNCED)
        dao.upsertChecklist(listOf(pending, synced))

        dao.mergeRemoteChecklist(
            reservationId = "reservation-1",
            remoteItems = listOf(checklist("remote", TripChecklistEntity.SYNCED))
        )

        val visible = dao.observeChecklist("reservation-1").first()
        assertTrue(visible.any { it.id == "pending" })
        assertTrue(visible.any { it.id == "remote" })
        assertTrue(visible.none { it.id == "old-synced" })
    }

    @Test
    fun `oculta un elemento marcado para eliminar`() = runBlocking {
        dao.upsertChecklistItem(
            checklist("deleted", TripChecklistEntity.PENDING_DELETE).copy(isDeleted = true)
        )

        assertTrue(dao.observeChecklist("reservation-1").first().isEmpty())
        assertEquals(1, dao.getPendingChecklist("client-1").size)
    }

    private fun trip(id: String, clientId: String) = TripEntity(
        reservationId = id,
        quotationId = "quotation-$id",
        clientId = clientId,
        agentId = "agent-1",
        destination = "Cusco",
        originCity = "Lima",
        departureAtMillis = 1_800_000_000_000L,
        returnAtMillis = 1_800_086_400_000L,
        travelerCount = 2,
        totalAmount = 1_500.0,
        currency = "PEN",
        reservationStatus = "Pagado",
        cachedAtMillis = 1L
    )

    private fun checklist(id: String, state: String) = TripChecklistEntity(
        id = id,
        reservationId = "reservation-1",
        clientId = "client-1",
        title = id,
        category = "Equipaje",
        isCompleted = false,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        syncState = state
    )
}
