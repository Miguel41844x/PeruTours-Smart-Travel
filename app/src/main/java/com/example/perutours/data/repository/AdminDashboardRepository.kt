package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.model.AdminMetrics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val adminCacheDao: AdminCacheDao? = null
) {
    // Stub combinando Firestore y Room (caché local)
    suspend fun fetchAdminMetrics(): AdminMetrics {
        return try {
            // Intento de consulta a Firestore (Simulado / Estructura base)
            val reservationsSnapshot = firestore.collection("reservations").get().await()
            var revenue = 0.0
            val statusCount = mutableMapOf<String, Int>()

            for (doc in reservationsSnapshot.documents) {
                val amount = doc.getDouble("totalAmount") ?: 0.0
                val status = doc.getString("status") ?: "Desconocido"
                revenue += amount
                statusCount[status] = (statusCount[status] ?: 0) + 1
            }

            val metrics = AdminMetrics(
                totalRevenue = revenue,
                totalReservations = reservationsSnapshot.size(),
                activeCustomers = 0,
                statusSummary = statusCount
            )

            // Guardar en caché local Room
            adminCacheDao?.cacheMetrics(
                AdminMetricsEntity(
                    totalRevenue = metrics.totalRevenue,
                    totalReservations = metrics.totalReservations,
                    updatedAtMillis = metrics.updatedAtMillis
                )
            )

            metrics
        } catch (_: Exception) {
            // Fallback a caché local si falla la red
            val cached = adminCacheDao?.getCachedMetrics()
            AdminMetrics(
                totalRevenue = cached?.totalRevenue ?: 0.0,
                totalReservations = cached?.totalReservations ?: 0,
                updatedAtMillis = cached?.updatedAtMillis ?: 0L
            )
        }
    }
}
