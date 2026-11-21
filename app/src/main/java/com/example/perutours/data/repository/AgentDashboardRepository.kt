package com.example.perutours.data.repository

import com.example.perutours.data.model.QuotationStatusMetric
import com.example.perutours.data.model.TravelRequestStatus
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class AgentDashboard(
    val name: String,
    val photoUrl: String,
    val statuses: List<QuotationStatusMetric>,
    val pendingPayments: Int
)

class AgentDashboardRepository(
    private val auth: FirebaseAuth,
    private val profiles: ProfileRepository,
    private val requests: TravelRequestDataSource,
    private val payments: PaymentReviewDataSource
) {
    suspend fun fetch(): AgentDashboard = coroutineScope {
        val uid = requireNotNull(auth.currentUser?.uid) { "Debes iniciar sesión." }
        val profile = async { profiles.getProfile(uid) }
        val portfolio = async { requests.getAllRequests() }
        val pending = async { payments.getPendingPaymentsForAgent() }
        AgentDashboard(
            name = profile.await().name,
            photoUrl = profile.await().photoUrl,
            statuses = portfolio.await().groupingBy { it.workflowStatus }.eachCount()
                .filterKeys { it != TravelRequestStatus.UNKNOWN }
                .map { (status, count) -> QuotationStatusMetric(status.label, count) },
            pendingPayments = pending.await().size
        )
    }
}
