package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.local.ReservationCacheDao
import com.example.perutours.data.local.ReservationEntity
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class AdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val adminCacheDao: AdminCacheDao? = null,
    private val reservationCacheDao: ReservationCacheDao? = null
) {
    suspend fun fetchAdminMetrics(): AdminMetrics {
        return try {
            // Reservas: solo para conteos por estado y clientes (no para dinero).
            val reservations = firestore.collection("reservations").get().await()
            val statusCount = mutableMapOf<String, Int>()
            val distinctCustomers = mutableSetOf<String>()

            for (doc in reservations.documents) {
                val status = doc.getString("status") ?: "Confirmada"
                statusCount[status] = (statusCount[status] ?: 0) + 1
                val clientId = doc.getString("clientId").orEmpty()
                if (clientId.isNotBlank()) distinctCustomers.add(clientId)
            }

            // Ingresos: pagos aprobados, igual que el dashboard del gerente.
            val approvedSales = firestore.collection("payments").get().await()
                .documents
                .mapNotNull { it.toApprovedSaleOrNull() }
                .filter { it.currency.equals("PEN", ignoreCase = true) }

            // Últimos 6 meses, con 0 en los meses sin ventas.
            val monthlyRevenue = SalesAnalyticsCalculator()
                .monthlyRevenue(approvedSales)
                .associate { it.label to it.amount }

            val metrics = AdminMetrics(
                totalRevenue = approvedSales.sumOf { it.amount },
                totalReservations = reservations.size(),
                activeCustomers = distinctCustomers.size,
                statusSummary = statusCount,
                monthlyRevenue = monthlyRevenue,
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

            query = if (!statusFilter.isNullOrBlank() && statusFilter != "Todos") {
                query.whereEqualTo("status", statusFilter)
                    .orderBy("createdAtMillis", Query.Direction.DESCENDING)
            } else {
                query.orderBy("createdAtMillis", Query.Direction.DESCENDING)
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
            ).orEmpty()
            if (cachedEntities.isEmpty()) {
                throw e
            }
            Pair(cachedEntities.map { it.toReservation() }, null)
        }
    }
}
