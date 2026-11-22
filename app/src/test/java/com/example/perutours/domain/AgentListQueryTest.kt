package com.example.perutours.domain

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TravelRequestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentListQueryTest {
    @Test
    fun requestSearchIgnoresAccentsAndOrdersUpcomingDepartures() {
        val recent = TravelRequest(id = "a", destination = "Junín, Perú", departureAtMillis = 200)
        val upcoming = recent.copy(id = "b", departureAtMillis = 100)
        val quoted = recent.copy(id = "c", status = TravelRequestStatus.QUOTED.wireValue)
        val query = AgentRequestQuery(text = "junin", upcomingFirst = true)
        assertEquals(listOf("b", "a"), query.matching(listOf(recent, quoted, upcoming)).map { it.id })
    }

    @Test
    fun paymentSearchCombinesMethodAndClientAndShowsOldestFirst() {
        val payment = Payment(id = "a", clientId = "client", method = "Yape", submittedAtMillis = 200)
        val older = payment.copy(id = "b", submittedAtMillis = 100)
        val plin = payment.copy(id = "c", method = "Plin")
        val query = AgentPaymentQuery(text = "lucia", method = "Yape")
        assertEquals(
            listOf("b", "a"),
            query.matching(listOf(payment, older, plin), mapOf("client" to "Lucía Demo")).map { it.id }
        )
    }
}
