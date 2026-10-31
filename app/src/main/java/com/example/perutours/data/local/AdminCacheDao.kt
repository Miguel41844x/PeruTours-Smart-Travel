package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AdminCacheDao {
    @Query("SELECT * FROM admin_metrics_cache LIMIT 1")
    suspend fun getCachedMetrics(): AdminMetricsEntity?

    @Upsert
    suspend fun cacheMetrics(metrics: AdminMetricsEntity)
}

@Entity(tableName = "admin_metrics_cache")
data class AdminMetricsEntity(
    @PrimaryKey
    val id: String = "primary_metrics",
    val totalRevenue: Double = 0.0,
    val totalReservations: Int = 0,
    val activeCustomers: Int = 0,
    val statusSummaryEncoded: String = "",
    val monthlyRevenueEncoded: String = "",
    val updatedAtMillis: Long = 0L
) {
    fun getStatusSummaryMap(): Map<String, Int> {
        if (statusSummaryEncoded.isBlank()) return emptyMap()
        return statusSummaryEncoded.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) parts[0] to (parts[1].toIntOrNull() ?: 0) else null
        }.toMap()
    }

    fun getMonthlyRevenueMap(): Map<String, Double> {
        if (monthlyRevenueEncoded.isBlank()) return emptyMap()
        return monthlyRevenueEncoded.split(";").mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size == 2) parts[0] to (parts[1].toDoubleOrNull() ?: 0.0) else null
        }.toMap()
    }

    companion object {
        fun fromMaps(
            totalRevenue: Double,
            totalReservations: Int,
            activeCustomers: Int,
            statusSummary: Map<String, Int>,
            monthlyRevenue: Map<String, Double>,
            updatedAtMillis: Long
        ): AdminMetricsEntity {
            val statusEncoded = statusSummary.entries.joinToString(";") { "${it.key}=${it.value}" }
            val monthlyEncoded = monthlyRevenue.entries.joinToString(";") { "${it.key}=${it.value}" }
            return AdminMetricsEntity(
                id = "primary_metrics",
                totalRevenue = totalRevenue,
                totalReservations = totalReservations,
                activeCustomers = activeCustomers,
                statusSummaryEncoded = statusEncoded,
                monthlyRevenueEncoded = monthlyEncoded,
                updatedAtMillis = updatedAtMillis
            )
        }
    }
}
