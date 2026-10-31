package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.local.ReservationCacheDao
import com.example.perutours.data.local.ReservationEntity
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val adminCacheDao: AdminCacheDao? = null,
    private val reservationCacheDao: ReservationCacheDao? = null
) {
    suspend fun fetchAdminMetrics(): AdminMetrics {
        val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
        return try {
            val snapshot = firestore.collection("reservations").get().await()
            var paidRevenue = 0.0
            val statusCount = mutableMapOf<String, Int>()
            val distinctCustomers = mutableSetOf<String>()
            val monthlyRevRaw = mutableMapOf<String, Double>()
            val sortKeyFormat = SimpleDateFormat("yyyy-MM", peruLocale)
            val displayKeyFormat = SimpleDateFormat("MMM yyyy", peruLocale)

            for (doc in snapshot.documents) {
                val amount = doc.getDouble("totalAmount") ?: 0.0
                val status = doc.getString("status") ?: "Confirmada"
                val clientId = doc.getString("clientId") ?: ""
                val createdAt = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()

                statusCount[status] = (statusCount[status] ?: 0) + 1
                if (clientId.isNotBlank()) {
                    distinctCustomers.add(clientId)
                }

                // Total revenue and monthly revenue count ONLY "Pagado"
                if (status.equals("Pagado", ignoreCase = true)) {
                    paidRevenue += amount
                    val sortKey = sortKeyFormat.format(Date(createdAt))
                    monthlyRevRaw[sortKey] = (monthlyRevRaw[sortKey] ?: 0.0) + amount
                }
            }

            // Sort monthly revenue chronologically by yyyy-MM
            val sortedMonthlyRevenue = monthlyRevRaw.entries
                .sortedBy { it.key }
                .associate { entry ->
                    val date = sortKeyFormat.parse(entry.key) ?: Date()
                    val label = displayKeyFormat.format(date).replaceFirstChar { it.uppercase() }
                    label to entry.value
                }

            val metrics = AdminMetrics(
                totalRevenue = paidRevenue,
                totalReservations = snapshot.size(),
                activeCustomers = distinctCustomers.size,
                statusSummary = statusCount,
                monthlyRevenue = sortedMonthlyRevenue,
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
        lastDocument: DocumentSnapshot? = null
    ): Pair<List<Reservation>, DocumentSnapshot?> {
        return try {
            var query: Query = firestore.collection("reservations")
                .orderBy("status")
                .orderBy("createdAtMillis", Query.Direction.DESCENDING)

            if (!statusFilter.isNullOrBlank() && statusFilter != "Todos") {
                query = query.whereEqualTo("status", statusFilter)
            }

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.limit(limit).get().await()
            val reservations = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
            val lastDoc = snapshot.documents.lastOrNull()

            if (reservations.isNotEmpty()) {
                reservationCacheDao?.cacheReservations(
                    reservations.map { ReservationEntity.fromReservation(it) }
                )
            }

            Pair(reservations, lastDoc)
        } catch (e: Exception) {
            val cachedEntities = reservationCacheDao?.getCachedReservations(
                status = statusFilter ?: "Todos",
                limit = limit.toInt()
            )
            val reservations = cachedEntities?.map { it.toReservation() } ?: emptyList()
            if (reservations.isEmpty() && cachedEntities == null) {
                throw e
            }
            Pair(reservations, null)
        }
    }
}
