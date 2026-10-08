package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.data.repository.TravelRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentRequestsViewModel(
    private val repository: TravelRequestRepository = TravelRequestRepository()
) : ViewModel() {

    private val _requests =
        MutableStateFlow<List<TravelRequest>>(emptyList())

    val requests: StateFlow<List<TravelRequest>> =
        _requests.asStateFlow()

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading.asStateFlow()

    private val _error =
        MutableStateFlow<String?>(null)

    val error: StateFlow<String?> =
        _error.asStateFlow()

    init {
        loadRequests()
    }

    fun loadRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                _requests.value = repository.getPendingRequests()
            } catch (e: Exception) {
                _error.value =
                    e.localizedMessage
                        ?: "No se pudieron cargar las solicitudes."
            } finally {
                _isLoading.value = false
            }
        }
    }
}