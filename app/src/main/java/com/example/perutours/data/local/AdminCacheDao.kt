package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.example.perutours.data.model.AdminMetrics
import com.google.gson.Gson

@Dao
interface AdminCacheDao {
    @Query("SELECT * FROM admin_metrics_cache LIMIT 1")
    suspend fun getCachedMetrics(): AdminMetricsEntity?

    @Upsert
    suspend fun cacheMetrics(metrics: AdminMetricsEntity)
}

@Entity(tableName = "admin_metrics_cache")
data class AdminMetricsEntity(
    @PrimaryKey val id: String = "catalog_overview",
    val metricsJson: String,
    val updatedAtMillis: Long
) {
    fun toMetrics(): AdminMetrics = Gson().fromJson(metricsJson, AdminMetrics::class.java)
        .copy(isCached = true, updatedAtMillis = updatedAtMillis)

    companion object {
        fun fromMetrics(metrics: AdminMetrics) = AdminMetricsEntity(
            metricsJson = Gson().toJson(metrics), updatedAtMillis = metrics.updatedAtMillis
        )
    }
}
