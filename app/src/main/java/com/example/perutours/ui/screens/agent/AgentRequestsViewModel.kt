package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TravelRequest
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
                // Ahora trae TODAS las solicitudes (Pendientes y Cotizadas)
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
            val isPending = request.status.contains("Pendiente", ignoreCase = true)
            if (isPending) {
                onNavigateToCreate(request.id)
            } else {
                val quotation = quotationRepository.getQuotationByRequestId(request.id)
                if (quotation != null) {
                    onNavigateToDetail(quotation.id)
                } else {
                    onNavigateToCreate(request.id)
                }
            }
        }
    }
}
