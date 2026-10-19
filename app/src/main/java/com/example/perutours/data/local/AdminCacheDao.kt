package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AdminCacheDao {
    // Stub para caché local de métricas administrativas en Room
    @Query("SELECT * FROM admin_metrics_cache LIMIT 1")
    suspend fun getCachedMetrics(): AdminMetricsEntity?

    @Upsert
    suspend fun cacheMetrics(metrics: AdminMetricsEntity)
}

// Entidad de Room provisional para caché local
@androidx.room.Entity(tableName = "admin_metrics_cache")
data class AdminMetricsEntity(
    @androidx.room.PrimaryKey
    val id: String = "primary_metrics",
    val totalRevenue: Double = 0.0,
    val totalReservations: Int = 0,
    val activeCustomers: Int = 0,
    val updatedAtMillis: Long = 0L
)
