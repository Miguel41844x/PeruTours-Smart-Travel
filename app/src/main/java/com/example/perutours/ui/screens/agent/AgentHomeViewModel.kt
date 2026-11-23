package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.data.repository.AgentDashboardDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AgentHomeViewModel(private val repository: AgentDashboardDataSource) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentHomeUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val data = repository.fetch()
                ensureActive()
                _uiState.value = AgentHomeUiState(
                    agentName = data.name.ifBlank { "Agente" },
                    photoUrl = data.photoUrl,
                    pendingCount = data.statuses.firstOrNull {
                        it.status == TravelRequestStatus.SUBMITTED.label
                    }?.count ?: 0,
                    quotedCount = data.statuses.firstOrNull {
                        it.status == TravelRequestStatus.QUOTED.label
                    }?.count ?: 0,
                    isLoadingCounts = false,
                    statuses = data.statuses,
                    pendingPayments = data.pendingPayments,
                    changesRequested = data.statuses.firstOrNull {
                        it.status == TravelRequestStatus.CHANGES_REQUESTED.label
                    }?.count ?: 0,
                    actionRequests = data.actionRequests,
                    upcomingTrips = data.upcomingTrips,
                    updatedAtMillis = data.updatedAtMillis
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "No pudimos actualizar tu cartera. Inténtalo nuevamente."
                    )
                }
            } finally {
                _uiState.update { it.copy(isRefreshing = false, isLoadingCounts = false) }
            }
        }
    }
}
