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
class DestinationDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: DestinationDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Base de datos SQLite en memoria (rápida y aislada para pruebas)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.destinationDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveDestinations() = runBlocking {
        val entity = DestinationEntity(
            id = "1",
            title = "Cusco Mágico",
            location = "Cusco, Perú",
            durationText = "3 días",
            durationDays = 3,
            priceAmount = 550.0,
            ratingScore = 4.9,
            imageUrl = "http://example.com/cusco.jpg"
        )

        dao.insertAll(listOf(entity))
        val list = dao.getAllDestinations().first()

        assertEquals(1, list.size)
        assertEquals("Cusco Mágico", list[0].title)
        assertEquals(550.0, list[0].priceAmount, 0.0)
    }

    @Test
    fun syncRemoteDestinationsReplacesOldDataAtomically() = runBlocking {
        val oldItem = DestinationEntity(
            id = "old_1",
            title = "Antiguo",
            location = "Lima",
            durationText = "1 día",
            durationDays = 1,
            priceAmount = 100.0,
            ratingScore = 4.0,
            imageUrl = ""
        )
        dao.insertAll(listOf(oldItem))

        val newItem = DestinationEntity(
            id = "new_2",
            title = "Nuevo Destino",
            location = "Arequipa",
            durationText = "2 días",
            durationDays = 2,
            priceAmount = 300.0,
            ratingScore = 4.8,
            imageUrl = ""
        )

        // Ejecutar transacción atómica
        dao.syncRemoteDestinations(listOf(newItem))
        val currentList = dao.getAllDestinations().first()

        assertEquals(1, currentList.size)
        assertEquals("new_2", currentList[0].id)
        assertEquals("Nuevo Destino", currentList[0].title)
    }
}