package com.example.perutours.ui.screens.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.SalesAnalytics
import com.example.perutours.data.repository.SalesDashboardDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SalesDashboardUiState(
    val isLoading: Boolean = false,
    val analytics: SalesAnalytics = SalesAnalytics(),
    val errorMessage: String? = null
)

class SalesDashboardViewModel(
    private val repository: SalesDashboardDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(SalesDashboardUiState())
    val uiState: StateFlow<SalesDashboardUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { repository.fetchSalesAnalytics() }
                .onSuccess { analytics ->
                    _uiState.update {
                        it.copy(isLoading = false, analytics = analytics)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "No pudimos actualizar las estadísticas. Revisa tu conexión."
                        )
                    }
                }
        }
    }
}
