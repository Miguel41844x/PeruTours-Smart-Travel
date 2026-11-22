package com.example.perutours.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.perutours.data.model.FavoriteItem
import com.example.perutours.data.model.FavoriteType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class FavoriteDaoTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val item = FavoriteItem("same-id", FavoriteType.DESTINATION, "same-id",
        "Arequipa", "Arequipa", "Destino guardado sin conexión.", "")

    @Test fun usersAndTypesDoNotShareOrOverwriteTheirFavorites() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val dao = db.favoriteDao()
            dao.upsert(item.toEntity("first"))
            dao.upsert(item.copy(type = FavoriteType.PACKAGE).toEntity("first"))
            dao.upsert(item.toEntity("second"))
            assertEquals(2, dao.observeAll("first").first().size)
            assertFalse(dao.toggle(item.toEntity("first")))
            assertEquals(FavoriteType.PACKAGE.name, dao.observeAll("first").first().single().kind)
            assertEquals(1, dao.observeAll("second").first().size)
            assertTrue(dao.observeAll("unknown").first().isEmpty())
        } finally { db.close() }
    }

    @Test fun dataSurvivesClosingTheDatabaseAndRepeatedSavesDoNotDuplicateIt() = runBlocking {
        val name = "favorites-persistence-test"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        try {
            db.favoriteDao().upsert(item.toEntity("client"))
            db.favoriteDao().upsert(item.toEntity("client"))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
            assertEquals(item, db.favoriteDao().observeAll("client").first().single().toModel())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
