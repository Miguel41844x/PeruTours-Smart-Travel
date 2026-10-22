package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Query(
        "SELECT * FROM trips WHERE clientId = :clientId " +
            "ORDER BY departureAtMillis ASC, cachedAtMillis DESC"
    )
    fun observeTrips(clientId: String): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE reservationId = :reservationId LIMIT 1")
    fun observeTrip(reservationId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE reservationId = :reservationId LIMIT 1")
    suspend fun getTrip(reservationId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrips(trips: List<TripEntity>)

    @Query("DELETE FROM trips WHERE clientId = :clientId AND reservationId NOT IN (:activeIds)")
    suspend fun deleteMissingTrips(clientId: String, activeIds: List<String>)

    @Query("DELETE FROM trips WHERE clientId = :clientId")
    suspend fun deleteAllTrips(clientId: String)

    @Transaction
    suspend fun replaceTrips(clientId: String, trips: List<TripEntity>) {
        if (trips.isEmpty()) {
            deleteAllTrips(clientId)
        } else {
            upsertTrips(trips)
            deleteMissingTrips(clientId, trips.map(TripEntity::reservationId))
        }
    }

    @Query(
        "SELECT * FROM trip_itinerary_items WHERE reservationId = :reservationId " +
            "ORDER BY dayIndex ASC, sortOrder ASC, startAtMillis ASC"
    )
    fun observeItinerary(reservationId: String): Flow<List<TripItineraryEntity>>

    @Query("SELECT * FROM trip_itinerary_items WHERE reservationId = :reservationId")
    suspend fun getItinerary(reservationId: String): List<TripItineraryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItinerary(items: List<TripItineraryEntity>)

    @Query("DELETE FROM trip_itinerary_items WHERE reservationId = :reservationId")
    suspend fun deleteItinerary(reservationId: String)

    @Transaction
    suspend fun replaceItinerary(
        reservationId: String,
        items: List<TripItineraryEntity>
    ) {
        deleteItinerary(reservationId)
        if (items.isNotEmpty()) upsertItinerary(items)
    }

    @Query(
        "SELECT * FROM trip_checklist_items " +
            "WHERE reservationId = :reservationId AND isDeleted = 0 " +
            "ORDER BY isCompleted ASC, createdAtMillis ASC"
    )
    fun observeChecklist(reservationId: String): Flow<List<TripChecklistEntity>>

    @Query("SELECT * FROM trip_checklist_items WHERE id = :itemId LIMIT 1")
    suspend fun getChecklistItem(itemId: String): TripChecklistEntity?

    @Query(
        "SELECT * FROM trip_checklist_items WHERE clientId = :clientId " +
            "AND syncState != 'SYNCED' ORDER BY updatedAtMillis ASC"
    )
    suspend fun getPendingChecklist(clientId: String): List<TripChecklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecklist(items: List<TripChecklistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChecklistItem(item: TripChecklistEntity)

    @Query(
        "UPDATE trip_checklist_items SET syncState = 'SYNCED', isDeleted = 0 " +
            "WHERE id = :itemId"
    )
    suspend fun markChecklistSynced(itemId: String)

    @Query("DELETE FROM trip_checklist_items WHERE id = :itemId")
    suspend fun deleteChecklistPermanently(itemId: String)

    @Query(
        "DELETE FROM trip_checklist_items WHERE reservationId = :reservationId " +
            "AND syncState = 'SYNCED'"
    )
    suspend fun deleteSyncedChecklist(reservationId: String)

    @Transaction
    suspend fun mergeRemoteChecklist(
        reservationId: String,
        remoteItems: List<TripChecklistEntity>
    ) {
        deleteSyncedChecklist(reservationId)
        if (remoteItems.isNotEmpty()) upsertChecklist(remoteItems)
    }

    @Query(
        "SELECT * FROM trip_incidents WHERE reservationId = :reservationId " +
            "ORDER BY createdAtMillis DESC"
    )
    fun observeIncidents(reservationId: String): Flow<List<TripIncidentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIncidents(incidents: List<TripIncidentEntity>)

    @Query("DELETE FROM trip_incidents WHERE reservationId = :reservationId")
    suspend fun deleteIncidents(reservationId: String)

    @Transaction
    suspend fun replaceIncidents(
        reservationId: String,
        incidents: List<TripIncidentEntity>
    ) {
        deleteIncidents(reservationId)
        if (incidents.isNotEmpty()) upsertIncidents(incidents)
    }
}
