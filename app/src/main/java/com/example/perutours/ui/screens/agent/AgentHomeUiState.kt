package com.example.perutours.ui.screens.agent

import com.example.perutours.data.model.QuotationStatusMetric
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequest

data class AgentHomeUiState(
    val agentName: String = "Agente",
    val photoUrl: String = "",
    val pendingCount: Int = 0,
    val quotedCount: Int = 0,
    val isLoadingCounts: Boolean = true,
    val isRefreshing: Boolean = false,
    val statuses: List<QuotationStatusMetric> = emptyList(),
    val pendingPayments: Int = 0,
    val changesRequested: Int = 0,
    val actionRequests: List<TravelRequest> = emptyList(),
    val upcomingTrips: List<Reservation> = emptyList(),
    val updatedAtMillis: Long = 0L,
    val errorMessage: String? = null
)
