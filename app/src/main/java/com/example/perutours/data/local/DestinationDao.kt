package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {

    @Query("SELECT * FROM destinations ORDER BY title ASC")
    fun getAllDestinations(): Flow<List<DestinationEntity>>

    @Query("SELECT * FROM destinations ORDER BY title ASC")
    suspend fun getAllDestinationsSync(): List<DestinationEntity>

    @Query("SELECT MAX(updatedAtMillis) FROM destinations")
    suspend fun getLastUpdatedTimestamp(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(destinations: List<DestinationEntity>)

    @Query("DELETE FROM destinations")
    suspend fun clearAll()

    /**
     * Reemplazo atómico dentro de una transacción Room:
     * Si la API eliminó un paquete obsoleto, se limpia y se repuebla en una sola
     * operación para no dejar la UI en blanco ni inconsistente.
     */
    @Transaction
    suspend fun syncRemoteDestinations(destinations: List<DestinationEntity>) {
        clearAll()
        insertAll(destinations)
    }
}