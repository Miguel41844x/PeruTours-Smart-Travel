package com.example.perutours.data.repository

import com.example.perutours.data.local.AdminCacheDao
import com.example.perutours.data.local.AdminMetricsEntity
import com.example.perutours.data.local.ReservationCacheDao
import com.example.perutours.data.local.ReservationEntity
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.model.TourProvider
import com.example.perutours.domain.analytics.AdminCatalogCalculator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

interface AdminDashboardDataSource {
    suspend fun fetchAdminMetrics(): AdminMetrics
    suspend fun fetchReservations(
        limit: Long = 15, statusFilter: String? = null, lastDocument: DocumentSnapshot? = null
    ): Pair<List<Reservation>, DocumentSnapshot?>
}

class AdminDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val adminCacheDao: AdminCacheDao? = null,
    private val reservationCacheDao: ReservationCacheDao? = null,
    private val calculator: AdminCatalogCalculator = AdminCatalogCalculator()
) : AdminDashboardDataSource {
    override suspend fun fetchAdminMetrics(): AdminMetrics = try {
        val metrics = coroutineScope {
            val providers = async { firestore.collection("providers").get(Source.SERVER).await() }
            val services = async { firestore.collection("catalog_services").get(Source.SERVER).await() }
            val destinations = async { firestore.collection("destinations").get(Source.SERVER).await() }
            val packages = async { firestore.collection("packages").get(Source.SERVER).await() }
            val users = async { firestore.collection("users").get(Source.SERVER).await() }
            calculator.calculate(
                providers = providers.await().documents.mapNotNull { doc ->
                    doc.toObject(TourProvider::class.java)?.copy(id = doc.id)
                },
                services = services.await().documents.mapNotNull { doc ->
                    doc.toObject(CatalogService::class.java)?.copy(id = doc.id)
                },
                destinations = destinations.await().documents.mapNotNull { doc ->
                    doc.toObject(TourDestination::class.java)?.copy(id = doc.id)
                },
                packages = packages.await().documents.mapNotNull { doc ->
                    doc.toObject(TourPackage::class.java)?.copy(id = doc.id)
                },
                roles = users.await().documents.map { it.getString("role").orEmpty() }
            )
        }
        adminCacheDao?.cacheMetrics(AdminMetricsEntity.fromMetrics(metrics))
        metrics
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!error.isConnectionFailure()) throw error
        adminCacheDao?.getCachedMetrics()?.toMetrics() ?: throw error
    }

    override suspend fun fetchReservations(
        limit: Long, statusFilter: String?, lastDocument: DocumentSnapshot?
    ): Pair<List<Reservation>, DocumentSnapshot?> = try {
        var query: Query = firestore.collection("reservations")
        if (!statusFilter.isNullOrBlank() && statusFilter != "Todos") {
            query = query.whereEqualTo("status", statusFilter)
        }
        query = query.orderBy("createdAtMillis", Query.Direction.DESCENDING)
        if (lastDocument != null) query = query.startAfter(lastDocument)
        val snapshot = query.limit(limit).get(Source.SERVER).await()
        val reservations = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Reservation::class.java)?.copy(id = doc.id)
        }
        if (reservations.isNotEmpty()) {
            reservationCacheDao?.cacheReservations(reservations.map(ReservationEntity::fromReservation))
        }
        reservations to snapshot.documents.lastOrNull()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        if (!error.isConnectionFailure() || lastDocument != null) throw error
        val cached = reservationCacheDao?.getCachedReservations(
            statusFilter?.takeIf { it.isNotBlank() } ?: "Todos", limit.toInt()
        ).orEmpty()
        if (cached.isEmpty()) throw error
        cached.map(ReservationEntity::toReservation) to null
    }

    private fun Exception.isConnectionFailure(): Boolean =
        this is FirebaseFirestoreException &&
            (code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
}
