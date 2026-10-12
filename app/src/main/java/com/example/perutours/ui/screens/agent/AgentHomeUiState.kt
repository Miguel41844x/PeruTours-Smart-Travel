package com.example.perutours.ui.screens.agent

data class AgentHomeUiState(
    val agentName: String = "Agente",
    val photoUrl: String = "",
    val pendingCount: Int = 0,
    val quotedCount: Int = 0,
    val isLoadingCounts: Boolean = true
)
