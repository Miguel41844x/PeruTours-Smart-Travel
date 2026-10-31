package com.example.perutours.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.AdminDashboardRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val metrics: AdminMetrics = AdminMetrics(),
    val reservations: List<Reservation> = emptyList(),
    val selectedFilterStatus: String = "Todos",
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val errorMessage: String? = null
) {
    val formattedUpdatedAt: String
        get() {
            if (metrics.updatedAtMillis <= 0L) return "hace un momento"
            val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
            return SimpleDateFormat("dd MMM, HH:mm", peruLocale).format(Date(metrics.updatedAtMillis))
        }
}

class AdminDashboardViewModel(
    private val repository: AdminDashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private var lastDocumentSnapshot: DocumentSnapshot? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val metrics = repository.fetchAdminMetrics()
                val (reservations, lastDoc) = repository.fetchReservations(
                    limit = AdminDashboardConstants.DEFAULT_PAGE_SIZE.toLong(),
                    statusFilter = _uiState.value.selectedFilterStatus,
                    lastDocument = null
                )
                lastDocumentSnapshot = lastDoc
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        metrics = metrics,
                        reservations = reservations,
                        endReached = reservations.size < AdminDashboardConstants.DEFAULT_PAGE_SIZE
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
        lastDocumentSnapshot = null
        viewModelScope.launch {
            try {
                val (reservations, lastDoc) = repository.fetchReservations(
                    limit = AdminDashboardConstants.DEFAULT_PAGE_SIZE.toLong(),
                    statusFilter = status,
                    lastDocument = null
                )
                lastDocumentSnapshot = lastDoc
                _uiState.update {
                    it.copy(
                        reservations = reservations,
                        endReached = reservations.size < AdminDashboardConstants.DEFAULT_PAGE_SIZE
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
                val (moreReservations, lastDoc) = repository.fetchReservations(
                    limit = AdminDashboardConstants.DEFAULT_PAGE_SIZE.toLong(),
                    statusFilter = state.selectedFilterStatus,
                    lastDocument = lastDocumentSnapshot
                )
                lastDocumentSnapshot = lastDoc
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        reservations = state.reservations + moreReservations,
                        endReached = moreReservations.isEmpty() || moreReservations.size < AdminDashboardConstants.DEFAULT_PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}
