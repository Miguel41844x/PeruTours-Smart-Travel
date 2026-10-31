package com.example.perutours.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DestinationEntity::class,
        PlaceEntity::class,
        TripEntity::class,
        TripItineraryEntity::class,
        TripChecklistEntity::class,
        TripIncidentEntity::class,
        AdminMetricsEntity::class,
        ReservationEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun destinationDao(): DestinationDao

    abstract fun placeDao(): PlaceDao

    abstract fun tripDao(): TripDao

    abstract fun adminCacheDao(): AdminCacheDao

    abstract fun reservationCacheDao(): ReservationCacheDao

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

        // Migración de versión 3 a 4: tabla de puntos de interés cercanos
        // (datos de la API externa de Geoapify cacheados en Room).
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS places (
                        id TEXT PRIMARY KEY NOT NULL,
                        destinationId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        categories TEXT NOT NULL,
                        address TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        distanceMeters REAL NOT NULL,
                        fetchedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_places_destinationId ON places (destinationId)"
                )
            }
        }

        // Migración de versión 4 a 5: centro de viaje disponible sin conexión.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        reservationId TEXT PRIMARY KEY NOT NULL,
                        quotationId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        agentId TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        originCity TEXT NOT NULL,
                        departureAtMillis INTEGER NOT NULL,
                        returnAtMillis INTEGER NOT NULL,
                        travelerCount INTEGER NOT NULL,
                        totalAmount REAL NOT NULL,
                        currency TEXT NOT NULL,
                        reservationStatus TEXT NOT NULL,
                        cachedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_clientId ON trips (clientId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trips_agentId ON trips (agentId)")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trips_departureAtMillis " +
                        "ON trips (departureAtMillis)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_itinerary_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        reservationId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        agentId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        locationName TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        startAtMillis INTEGER NOT NULL,
                        endAtMillis INTEGER NOT NULL,
                        dayIndex INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        providerName TEXT NOT NULL,
                        status TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        source TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_itinerary_items_reservationId " +
                        "ON trip_itinerary_items (reservationId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_itinerary_items_clientId " +
                        "ON trip_itinerary_items (clientId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_itinerary_items_agentId " +
                        "ON trip_itinerary_items (agentId)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_checklist_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        reservationId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        syncState TEXT NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_checklist_items_reservationId " +
                        "ON trip_checklist_items (reservationId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_checklist_items_clientId " +
                        "ON trip_checklist_items (clientId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_checklist_items_syncState " +
                        "ON trip_checklist_items (syncState)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trip_incidents (
                        id TEXT PRIMARY KEY NOT NULL,
                        reservationId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        agentId TEXT NOT NULL,
                        category TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        status TEXT NOT NULL,
                        description TEXT NOT NULL,
                        locationName TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        agentResponse TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        updatedAtMillis INTEGER NOT NULL,
                        resolvedAtMillis INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_incidents_reservationId " +
                        "ON trip_incidents (reservationId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_incidents_clientId " +
                        "ON trip_incidents (clientId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_incidents_agentId " +
                        "ON trip_incidents (agentId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_trip_incidents_status " +
                        "ON trip_incidents (status)"
                )
            }
        }

        // Migración de versión 5 a 6: caché local de métricas del dashboard de administración.
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS admin_metrics_cache (
                        id TEXT PRIMARY KEY NOT NULL,
                        totalRevenue REAL NOT NULL,
                        totalReservations INTEGER NOT NULL,
                        activeCustomers INTEGER NOT NULL,
                        statusSummaryEncoded TEXT NOT NULL,
                        monthlyRevenueEncoded TEXT NOT NULL,
                        updatedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        // Migración de versión 6 a 7: caché local de reservas para el dashboard admin.
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reservation_cache (
                        id TEXT PRIMARY KEY NOT NULL,
                        quotationId TEXT NOT NULL,
                        requestId TEXT NOT NULL,
                        clientId TEXT NOT NULL,
                        agentId TEXT NOT NULL,
                        destination TEXT NOT NULL,
                        originCity TEXT NOT NULL,
                        departureAtMillis INTEGER NOT NULL,
                        returnAtMillis INTEGER NOT NULL,
                        travelerCount INTEGER NOT NULL,
                        totalAmount REAL NOT NULL,
                        currency TEXT NOT NULL,
                        validityDate TEXT NOT NULL,
                        conditions TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        paymentId TEXT NOT NULL,
                        paymentVoucherUrl TEXT NOT NULL,
                        paymentMethod TEXT NOT NULL,
                        paidAtMillis INTEGER,
                        cachedAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "perutours_db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
