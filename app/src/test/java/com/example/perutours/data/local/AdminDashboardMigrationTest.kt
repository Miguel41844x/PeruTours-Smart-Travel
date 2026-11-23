package com.example.perutours.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.perutours.data.model.AdminMetrics
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AdminDashboardMigrationTest {
    @Test fun upgradesTheDashboardCacheWithoutLosingFavorites() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "dashboard-migration-test"
        context.deleteDatabase(name)
        val schema = JsonParser.parseString(File("schemas/com.example.perutours.data.local.AppDatabase/9.json").readText())
            .asJsonObject.getAsJsonObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            schema.getAsJsonArray("entities").forEach { element ->
                val entity = element.asJsonObject
                val table = entity.get("tableName").asString
                old.execSQL(entity.get("createSql").asString.replace("\${TABLE_NAME}", table))
                entity.getAsJsonArray("indices")?.forEach { index ->
                    old.execSQL(index.asJsonObject.get("createSql").asString.replace("\${TABLE_NAME}", table))
                }
            }
            schema.getAsJsonArray("setupQueries").forEach { old.execSQL(it.asString) }
            old.execSQL("INSERT INTO favorites VALUES ('client', 'pack', 'PACKAGE', 'cusco', 'Viaje guardado', 'Cusco', '', '', '', '', '', 1)")
            old.version = 9
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_9_10).allowMainThreadQueries().build()
        try {
            database.openHelper.writableDatabase.query("SELECT title FROM favorites").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Viaje guardado", cursor.getString(0))
            }
            val metrics = AdminMetrics(totalProviders = 4, availableServices = 12,
                usersByRole = mapOf("CLIENT" to 8), updatedAtMillis = 123L)
            database.adminCacheDao().cacheMetrics(AdminMetricsEntity.fromMetrics(metrics))
            val restored = database.adminCacheDao().getCachedMetrics()!!.toMetrics()
            assertEquals(4, restored.totalProviders)
            assertEquals(8, restored.totalUsers)
            assertTrue(restored.isCached)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
