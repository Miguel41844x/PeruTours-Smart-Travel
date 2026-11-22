package com.example.perutours.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    @Test
    fun versionSevenPreservesPendingOfflineEdits() = verifyUpgrade(7)

    @Test
    fun versionEightPreservesPendingOfflineEdits() = verifyUpgrade(8)

    @Test
    fun experimentalVersionSixWithServiceLinksPreservesPendingOfflineEdits() = verifyUpgrade(6)

    private fun verifyUpgrade(oldVersion: Int) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration_" + oldVersion
        context.deleteDatabase(name)
        val relativeSchema = "schemas/com.example.perutours.data.local.AppDatabase/" + oldVersion + ".json"
        val schema = JSONObject(
            listOf(File(relativeSchema), File("app", relativeSchema)).first(File::isFile).readText()
        ).getJSONObject("database")
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(oldVersion) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        val entities = schema.getJSONArray("entities")
                        for (index in 0 until entities.length()) {
                            val entity = entities.getJSONObject(index)
                            val table = entity.getString("tableName")
                            val placeholder = "$" + "{TABLE_NAME}"
                            db.execSQL(entity.getString("createSql").replace(placeholder, table))
                            val indices = entity.optJSONArray("indices")
                            for (i in 0 until (indices?.length() ?: 0)) {
                                db.execSQL(indices!!.getJSONObject(i).getString("createSql").replace(placeholder, table))
                            }
                        }
                        val setup = schema.getJSONArray("setupQueries")
                        for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                        if (oldVersion == 6) {
                            db.execSQL("ALTER TABLE trip_itinerary_items ADD COLUMN reservationServiceId TEXT NOT NULL DEFAULT ''")
                            db.execSQL("DROP TABLE IF EXISTS admin_metrics_cache")
                        }
                        db.execSQL(
                            "INSERT INTO trip_checklist_items VALUES " +
                                "('pending-edit', 'reservation', 'client', 'Llevar DNI', 'Documentos', 1, 1, 2, 'PENDING_UPSERT', 0)"
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9)
            .allowMainThreadQueries().build()
        try {
            database.openHelper.writableDatabase.query(
                "SELECT title, syncState FROM trip_checklist_items WHERE id = 'pending-edit'"
            ).use {
                it.moveToFirst()
                assertEquals("Llevar DNI", it.getString(0))
                assertEquals("PENDING_UPSERT", it.getString(1))
            }
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
