package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.model.TravelRequestStatus
import com.example.perutours.domain.AgentRequestQuery
import com.example.perutours.data.repository.QuotationRepository
import com.example.perutours.data.repository.TravelRequestDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentRequestsViewModel(
    private val repository: TravelRequestDataSource,
    private val quotationRepository: QuotationRepository
) : ViewModel() {

    private val _requests = MutableStateFlow<List<TravelRequest>>(emptyList())
    val requests: StateFlow<List<TravelRequest>> = _requests.asStateFlow()
    private val _query = MutableStateFlow(AgentRequestQuery())
    val query = _query.asStateFlow()

    fun search(value: String) {
        _query.value = _query.value.copy(text = value.take(120), visibleLimit = 20)
    }

    fun filterStatus(status: TravelRequestStatus?) {
        _query.value = _query.value.copy(status = status, visibleLimit = 20)
    }

    fun toggleOrder() {
        _query.value = _query.value.copy(upcomingFirst = !_query.value.upcomingFirst, visibleLimit = 20)
    }

    fun loadMore() {
        _query.value = _query.value.copy(visibleLimit = _query.value.visibleLimit + 20)
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _requests.value = repository.getAllRequests()
            } catch (_: Exception) {
                _error.value = "Revisa tu conexión e inténtalo nuevamente."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openRequest(
        request: TravelRequest,
        onNavigateToCreate: (String) -> Unit,
        onNavigateToDetail: (String) -> Unit
    ) {
        viewModelScope.launch {
            _error.value = null
            runCatching {
                if (request.workflowStatus == TravelRequestStatus.SUBMITTED) {
                    repository.claimRequest(request.id)
                    request.id to null
                } else {
                    val quotation = quotationRepository.getQuotationByRequestId(request.id)
                    request.id to quotation?.id
                }
            }.onSuccess { (requestId, quotationId) ->
                if (quotationId == null) onNavigateToCreate(requestId)
                else onNavigateToDetail(quotationId)
            }.onFailure { error ->
                _error.value = error.localizedMessage ?: "No pudimos abrir la solicitud."
                loadRequests()
            }
        }
    }
}
