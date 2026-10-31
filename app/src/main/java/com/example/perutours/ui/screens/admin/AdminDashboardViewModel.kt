package com.example.perutours.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.AdminDashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val metrics: AdminMetrics = AdminMetrics(),
    val reservations: List<Reservation> = emptyList(),
    val selectedFilterStatus: String = "Todos",
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val errorMessage: String? = null
)

class AdminDashboardViewModel(
    private val repository: AdminDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val metrics = repository.fetchAdminMetrics()
                val reservations = repository.fetchReservations(
                    limit = 15,
                    statusFilter = _uiState.value.selectedFilterStatus
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        metrics = metrics,
                        reservations = reservations,
                        endReached = reservations.size < 15
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Error al cargar datos del dashboard"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun setFilterStatus(status: String) {
        if (_uiState.value.selectedFilterStatus == status) return
        _uiState.update { it.copy(selectedFilterStatus = status, endReached = false) }
        viewModelScope.launch {
            try {
                val reservations = repository.fetchReservations(
                    limit = 15,
                    statusFilter = status
                )
                _uiState.update {
                    it.copy(
                        reservations = reservations,
                        endReached = reservations.size < 15
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun loadMoreReservations() {
        val state = _uiState.value
        if (state.isLoadingMore || state.endReached) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val lastCreatedAt = state.reservations.lastOrNull()?.createdAtMillis
                val moreReservations = repository.fetchReservations(
                    limit = 15,
                    statusFilter = state.selectedFilterStatus,
                    lastCreatedAt = lastCreatedAt
                )
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        reservations = state.reservations + moreReservations,
                        endReached = moreReservations.isEmpty()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}
