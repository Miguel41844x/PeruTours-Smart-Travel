package com.example.perutours.domain

import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.data.model.Payment
import java.text.Normalizer
import java.util.Locale

fun String.searchKey(): String = Normalizer.normalize(trim(), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)

data class AgentRequestQuery(
    val text: String = "",
    val status: TravelRequestStatus? = TravelRequestStatus.SUBMITTED,
    val upcomingFirst: Boolean = false,
    val visibleLimit: Int = 20
) {
    fun matching(requests: List<TravelRequest>): List<TravelRequest> {
        val key = text.searchKey()
        val matching = requests.filter {
            (status == null || it.workflowStatus == status) &&
                listOf(it.id, it.destination, it.originCity, it.notes, it.userId)
                    .any { field -> field.searchKey().contains(key) }
        }
        return if (upcomingFirst) matching.sortedBy { it.departureAtMillis }
        else matching.sortedByDescending { it.createdAtMillis }
    }
}

data class AgentPaymentQuery(val text: String = "", val method: String? = null, val visibleLimit: Int = 20) {
    fun matching(payments: List<Payment>, clientNames: Map<String, String>): List<Payment> {
        val key = text.searchKey()
        return payments.filter {
            (method == null || it.method == method) &&
                listOf(it.id, it.reservationId, it.method, clientNames[it.clientId].orEmpty())
                    .any { field -> field.searchKey().contains(key) }
        }.sortedBy { it.submittedAtMillis }
    }
}
