package com.example.perutours.data.repository

import com.example.perutours.data.model.QuotationStatusMetric
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequestStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

data class AgentDashboard(
    val name: String,
    val photoUrl: String,
    val statuses: List<QuotationStatusMetric>,
    val pendingPayments: Int,
    val actionRequests: List<TravelRequest> = emptyList(),
    val upcomingTrips: List<Reservation> = emptyList(),
    val updatedAtMillis: Long = 0L
)

fun interface AgentDashboardDataSource {
    suspend fun fetch(): AgentDashboard
}

class AgentDashboardRepository(
    private val auth: FirebaseAuth,
    private val profiles: ProfileRepository,
    private val requests: TravelRequestDataSource,
    private val payments: PaymentReviewDataSource,
    private val trips: AgentTripDataSource
) : AgentDashboardDataSource {
    override suspend fun fetch(): AgentDashboard = coroutineScope {
        val uid = requireNotNull(auth.currentUser?.uid) { "Debes iniciar sesión." }
        val profile = async { profiles.getProfile(uid) }
        val portfolio = async { requests.getAllRequests() }
        val pending = async { payments.getPendingPaymentsForAgent() }
        val departures = async { trips.observeAssignedTrips().first() }
        val requests = portfolio.await()
        val now = System.currentTimeMillis()
        AgentDashboard(
            name = profile.await().name,
            photoUrl = profile.await().photoUrl,
            statuses = requests.groupingBy { it.workflowStatus }.eachCount()
                .filterKeys { it != TravelRequestStatus.UNKNOWN }
                .toSortedMap(compareBy { it.ordinal })
                .map { (status, count) -> QuotationStatusMetric(status.label, count) },
            pendingPayments = pending.await().size,
            actionRequests = requests.filter {
                it.workflowStatus == TravelRequestStatus.SUBMITTED ||
                    it.workflowStatus == TravelRequestStatus.CHANGES_REQUESTED
            }.sortedWith(compareBy<TravelRequest> {
                if (it.workflowStatus == TravelRequestStatus.CHANGES_REQUESTED) 0 else 1
            }.thenBy { it.createdAtMillis }).take(4),
            upcomingTrips = departures.await().filter { it.departureAtMillis >= now }
                .sortedBy { it.departureAtMillis },
            updatedAtMillis = now
        )
    }
}
