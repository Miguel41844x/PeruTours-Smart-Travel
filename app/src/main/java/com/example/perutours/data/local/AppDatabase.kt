package com.example.perutours.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [DestinationEntity::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 1 a 2 adaptando columnas numéricas y timestamp de expiración
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS destinations")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS destinations (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        location TEXT NOT NULL,
                        durationText TEXT NOT NULL,
                        durationDays INTEGER NOT NULL,
                        priceAmount REAL NOT NULL,
                        ratingScore REAL NOT NULL,
                        category TEXT NOT NULL,
                        imageUrl TEXT NOT NULL,
                        description TEXT,
                        updatedAtMillis INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Migración de versión 2 a 3: el catálogo proviene de Firestore, por
        // lo que la fila local ahora guarda el destino de origen y sus
        // coordenadas para consultar puntos de interés cercanos.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE destinations ADD COLUMN destinationId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE destinations ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE destinations ADD COLUMN latitude REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE destinations ADD COLUMN longitude REAL NOT NULL DEFAULT 0.0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perutours_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
