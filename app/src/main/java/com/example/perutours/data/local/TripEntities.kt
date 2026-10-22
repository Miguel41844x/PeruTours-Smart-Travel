package com.example.perutours.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [Index("clientId"), Index("agentId"), Index("departureAtMillis")]
)
data class TripEntity(
    @PrimaryKey val reservationId: String,
    val quotationId: String,
    val clientId: String,
    val agentId: String,
    val destination: String,
    val originCity: String,
    val departureAtMillis: Long,
    val returnAtMillis: Long,
    val travelerCount: Int,
    val totalAmount: Double,
    val currency: String,
    val reservationStatus: String,
    val cachedAtMillis: Long
)

@Entity(
    tableName = "trip_itinerary_items",
    indices = [Index("reservationId"), Index("clientId"), Index("agentId")]
)
data class TripItineraryEntity(
    @PrimaryKey val id: String,
    val reservationId: String,
    val clientId: String,
    val agentId: String,
    val title: String,
    val description: String,
    val category: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val dayIndex: Int,
    val sortOrder: Int,
    val providerName: String,
    val status: String,
    val notes: String,
    val source: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "trip_checklist_items",
    indices = [Index("reservationId"), Index("clientId"), Index("syncState")]
)
data class TripChecklistEntity(
    @PrimaryKey val id: String,
    val reservationId: String,
    val clientId: String,
    val title: String,
    val category: String,
    val isCompleted: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val syncState: String = SYNCED,
    val isDeleted: Boolean = false
) {
    companion object {
        const val SYNCED = "SYNCED"
        const val PENDING_UPSERT = "PENDING_UPSERT"
        const val PENDING_DELETE = "PENDING_DELETE"
    }
}

@Entity(
    tableName = "trip_incidents",
    indices = [Index("reservationId"), Index("clientId"), Index("agentId"), Index("status")]
)
data class TripIncidentEntity(
    @PrimaryKey val id: String,
    val reservationId: String,
    val clientId: String,
    val agentId: String,
    val category: String,
    val priority: String,
    val status: String,
    val description: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val agentResponse: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val resolvedAtMillis: Long?
)
