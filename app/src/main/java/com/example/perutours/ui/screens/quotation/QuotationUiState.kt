package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationServiceItem

data class QuotationFormState(
    val requestId: String = "",
    val clientId: String = "",
    val clientName: String = "",
    val clientEmail: String = "",
    val destination: String = "",
    val travelerCount: Int = 2,
    val selectedServices: List<QuotationServiceItem> = emptyList(),
    val marginPercent: Int = 15,
    val validityDays: Int = 5,
    val conditions: String = "Tarifa garantizada hasta fecha de validez. Modificaciones de fecha permitidas con 72h de anticipación.",
    val clientFcmToken: String = ""
)

sealed interface QuotationUiState {
    object Idle : QuotationUiState
    object Loading : QuotationUiState
    data class Success(val quotation: Quotation) : QuotationUiState
    data class Error(val message: String) : QuotationUiState
}