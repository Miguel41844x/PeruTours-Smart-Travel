package com.example.perutours.ui.screens.agent

import com.example.perutours.data.model.QuotationStatusMetric

data class AgentHomeUiState(
    val agentName: String = "Agente",
    val photoUrl: String = "",
    val pendingCount: Int = 0,
    val quotedCount: Int = 0,
    val isLoadingCounts: Boolean = true,
    val isRefreshing: Boolean = false,
    val statuses: List<QuotationStatusMetric> = emptyList(),
    val pendingPayments: Int = 0,
    val errorMessage: String? = null
)
