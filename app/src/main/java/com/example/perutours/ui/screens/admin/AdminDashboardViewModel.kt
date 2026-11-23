package com.example.perutours.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.AdminMetrics
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.AdminDashboardDataSource
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AdminDashboardUiState(
    val isLoadingMetrics: Boolean = false,
    val isLoadingReservations: Boolean = false,
    val metrics: AdminMetrics = AdminMetrics(),
    val reservations: List<Reservation> = emptyList(),
    val selectedFilterStatus: String = "Todos",
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val metricsErrorMessage: String? = null,
    val reservationsErrorMessage: String? = null
) {
    val isLoading: Boolean
        get() = isLoadingMetrics || isLoadingReservations

    val errorMessage: String?
        get() = listOfNotNull(metricsErrorMessage, reservationsErrorMessage)
            .distinct().joinToString("\n").takeIf { it.isNotBlank() }

    val formattedUpdatedAt: String
        get() {
            if (metrics.updatedAtMillis <= 0L) return "hace un momento"
            val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
            return SimpleDateFormat("dd MMM, HH:mm", peruLocale).format(Date(metrics.updatedAtMillis))
        }
}

class AdminDashboardViewModel(
    private val repository: AdminDashboardDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    private var lastDocumentSnapshot: DocumentSnapshot? = null
    private var reservationsJob: Job? = null
    private var loadMoreJob: Job? = null
    private var reservationsVersion = 0L

    init {
        loadData()
    }

    fun loadData() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoadingMetrics = true, metricsErrorMessage = null) }
        viewModelScope.launch {
            try {
                val metrics = repository.fetchAdminMetrics()
                ensureActive()
                _uiState.update { it.copy(metrics = metrics) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        metricsErrorMessage = "No pudimos cargar las métricas. Inténtalo nuevamente."
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoadingMetrics = false) }
            }
        }
        loadFirstPage(clearReservations = false)
    }

    fun refresh() {
        loadData()
    }

    fun setFilterStatus(status: String) {
        if (status !in AdminDashboardConstants.AVAILABLE_FILTERS ||
            _uiState.value.selectedFilterStatus == status) return
        _uiState.update { it.copy(selectedFilterStatus = status) }
        loadFirstPage(clearReservations = true)
    }

    private fun loadFirstPage(clearReservations: Boolean) {
        val version = ++reservationsVersion
        reservationsJob?.cancel()
        loadMoreJob?.cancel()
        lastDocumentSnapshot = null
        val status = _uiState.value.selectedFilterStatus
        _uiState.update {
            it.copy(
                isLoadingReservations = true,
                isLoadingMore = false,
                reservations = if (clearReservations) emptyList() else it.reservations,
                endReached = false,
                reservationsErrorMessage = null
            )
        }
        reservationsJob = viewModelScope.launch {
            try {
                val (reservations, lastDoc) = repository.fetchReservations(
                    limit = AdminDashboardConstants.DEFAULT_PAGE_SIZE.toLong(),
                    statusFilter = status,
                    lastDocument = null
                )
                ensureActive()
                if (version != reservationsVersion) return@launch
                lastDocumentSnapshot = lastDoc
                _uiState.update {
                    it.copy(
                        reservations = reservations,
                        endReached = lastDoc == null || reservations.size < AdminDashboardConstants.DEFAULT_PAGE_SIZE
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == reservationsVersion) {
                    _uiState.update {
                        it.copy(reservationsErrorMessage = "No pudimos cargar las reservas. Inténtalo nuevamente.")
                    }
                }
            } finally {
                if (version == reservationsVersion) {
                    _uiState.update { it.copy(isLoadingReservations = false) }
                }
            }
        }
    }

    fun loadMoreReservations() {
        val state = _uiState.value
        val cursor = lastDocumentSnapshot ?: return
        if (state.isLoading || state.isLoadingMore || state.endReached) return
        val version = reservationsVersion
        _uiState.update { it.copy(isLoadingMore = true, reservationsErrorMessage = null) }

        loadMoreJob = viewModelScope.launch {
            try {
                val (moreReservations, lastDoc) = repository.fetchReservations(
                    limit = AdminDashboardConstants.DEFAULT_PAGE_SIZE.toLong(),
                    statusFilter = state.selectedFilterStatus,
                    lastDocument = cursor
                )
                ensureActive()
                if (version != reservationsVersion) return@launch
                lastDocumentSnapshot = lastDoc
                _uiState.update {
                    it.copy(
                        reservations = (it.reservations + moreReservations).distinctBy { reservation -> reservation.id },
                        endReached = lastDoc == null || moreReservations.isEmpty() || moreReservations.size < AdminDashboardConstants.DEFAULT_PAGE_SIZE
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (version == reservationsVersion) {
                    _uiState.update {
                        it.copy(reservationsErrorMessage = "No pudimos cargar más reservas. Inténtalo nuevamente.")
                    }
                }
            } finally {
                if (version == reservationsVersion) {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
            }
        }
    }
}
