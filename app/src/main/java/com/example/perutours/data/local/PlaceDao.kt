package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/**
 * Acceso a los puntos de interés cacheados por destino.
 */
@Dao
interface PlaceDao {

    @Query("SELECT * FROM places WHERE destinationId = :destinationId ORDER BY distanceMeters ASC")
    suspend fun getPlacesForDestination(destinationId: String): List<PlaceEntity>

    @Query("SELECT MAX(fetchedAtMillis) FROM places WHERE destinationId = :destinationId")
    suspend fun getLastFetchedTimestamp(destinationId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<PlaceEntity>)

    @Query("DELETE FROM places WHERE destinationId = :destinationId")
    suspend fun clearForDestination(destinationId: String)

    /** Reemplazo atómico: limpia y repuebla los lugares de un destino. */
    @Transaction
    suspend fun syncPlacesForDestination(destinationId: String, places: List<PlaceEntity>) {
        clearForDestination(destinationId)
        insertAll(places)
    }
}
