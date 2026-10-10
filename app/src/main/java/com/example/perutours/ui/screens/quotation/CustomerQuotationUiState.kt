package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationObservation

sealed interface CustomerQuotationsUiState {
    data object Loading : CustomerQuotationsUiState
    data class Success(val quotations: List<Quotation>) : CustomerQuotationsUiState
    data class Error(val message: String) : CustomerQuotationsUiState
}

data class CustomerQuotationDetailUiState(
    val isLoading: Boolean = false,
    val quotation: Quotation? = null,
    val observations: List<QuotationObservation> = emptyList(),
    val errorMessage: String? = null
)

sealed interface QuotationResponseUiState {
    data object Idle : QuotationResponseUiState
    data object Saving : QuotationResponseUiState
    data class Success(
        val quotation: Quotation,
        val message: String
    ) : QuotationResponseUiState

    data class Error(val message: String) : QuotationResponseUiState
}
