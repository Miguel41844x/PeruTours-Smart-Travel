package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val adminCacheDao: AdminCacheDao? = null
) {
    suspend fun fetchAdminMetrics(): AdminMetrics {
        return try {
            val snapshot = firestore.collection("reservations").get().await()
            var revenue = 0.0
            val statusCount = mutableMapOf<String, Int>()
            val distinctCustomers = mutableSetOf<String>()
            val monthlyRev = mutableMapOf<String, Double>()
            val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
            val monthFormat = SimpleDateFormat("MMM", peruLocale)

            for (doc in snapshot.documents) {
                val amount = doc.getDouble("totalAmount") ?: 0.0
                val status = doc.getString("status") ?: "Confirmada"
                val clientId = doc.getString("clientId") ?: ""
                val createdAt = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()

                revenue += amount
                statusCount[status] = (statusCount[status] ?: 0) + 1
                if (clientId.isNotBlank()) {
                    distinctCustomers.add(clientId)
                }

                val monthKey = monthFormat.format(Date(createdAt)).replaceFirstChar { it.uppercase() }
                monthlyRev[monthKey] = (monthlyRev[monthKey] ?: 0.0) + amount
            }

            val metrics = AdminMetrics(
                totalRevenue = revenue,
                totalReservations = snapshot.size(),
                activeCustomers = distinctCustomers.size,
                statusSummary = statusCount,
                monthlyRevenue = monthlyRev,
                isCached = false,
                updatedAtMillis = System.currentTimeMillis()
            )

            adminCacheDao?.cacheMetrics(
                AdminMetricsEntity.fromMaps(
                    totalRevenue = metrics.totalRevenue,
                    totalReservations = metrics.totalReservations,
                    activeCustomers = metrics.activeCustomers,
                    statusSummary = metrics.statusSummary,
                    monthlyRevenue = metrics.monthlyRevenue,
                    updatedAtMillis = metrics.updatedAtMillis
                )
            )

            metrics
        } catch (e: Exception) {
            val cached = adminCacheDao?.getCachedMetrics()
            if (cached != null) {
                AdminMetrics(
                    totalRevenue = cached.totalRevenue,
                    totalReservations = cached.totalReservations,
                    activeCustomers = cached.activeCustomers,
                    statusSummary = cached.getStatusSummaryMap(),
                    monthlyRevenue = cached.getMonthlyRevenueMap(),
                    isCached = true,
                    updatedAtMillis = cached.updatedAtMillis
                )
            } else {
                throw e
            }
        }
    }

    suspend fun fetchReservations(
        limit: Long = 15,
        statusFilter: String? = null,
        lastCreatedAt: Long? = null
    ): List<Reservation> {
        return try {
            var query: Query = firestore.collection("reservations")
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)

            if (!statusFilter.isNullOrBlank() && statusFilter != "Todos") {
                query = query.whereEqualTo("status", statusFilter)
            }

            if (lastCreatedAt != null) {
                query = query.startAfter(lastCreatedAt)
            }

            val snapshot = query.limit(limit).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
