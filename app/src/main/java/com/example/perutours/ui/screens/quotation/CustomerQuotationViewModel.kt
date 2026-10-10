package com.example.perutours.ui.screens.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.repository.QuotationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerQuotationViewModel(
    private val repository: QuotationRepository = QuotationRepository()
) : ViewModel() {

    private val _quotationsState =
        MutableStateFlow<CustomerQuotationsUiState>(CustomerQuotationsUiState.Loading)
    val quotationsState: StateFlow<CustomerQuotationsUiState> = _quotationsState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerQuotationDetailUiState())
    val detailState: StateFlow<CustomerQuotationDetailUiState> = _detailState.asStateFlow()

    private val _responseState =
        MutableStateFlow<QuotationResponseUiState>(QuotationResponseUiState.Idle)
    val responseState: StateFlow<QuotationResponseUiState> = _responseState.asStateFlow()

    fun loadQuotations() {
        viewModelScope.launch {
            _quotationsState.value = CustomerQuotationsUiState.Loading
            _quotationsState.value = try {
                CustomerQuotationsUiState.Success(repository.getCurrentClientQuotations())
            } catch (exception: Exception) {
                CustomerQuotationsUiState.Error(
                    exception.localizedMessage ?: "No se pudieron cargar tus cotizaciones."
                )
            }
        }
    }

    fun loadQuotation(quotationId: String) {
        if (quotationId.isBlank()) {
            _detailState.value = CustomerQuotationDetailUiState(
                errorMessage = "No se recibió el identificador de la cotización."
            )
            return
        }

        viewModelScope.launch {
            _detailState.value = CustomerQuotationDetailUiState(isLoading = true)
            _detailState.value = try {
                val quotation = repository.getCurrentClientQuotationById(quotationId)
                    ?: throw IllegalStateException("No se encontró la cotización.")
                val observations = repository.getObservationHistory(quotationId)
                CustomerQuotationDetailUiState(
                    quotation = quotation,
                    observations = observations
                )
            } catch (exception: Exception) {
                CustomerQuotationDetailUiState(
                    errorMessage = exception.localizedMessage
                        ?: "No se pudo cargar la cotización."
                )
            }
        }
    }

    fun acceptQuotation(quotationId: String) {
        respond(
            successMessage = "Cotización aceptada. Tu reserva fue creada.",
            action = { repository.acceptQuotation(quotationId) }
        )
    }

    fun observeQuotation(quotationId: String, message: String) {
        val validationError = QuotationResponseValidator.validateObservation(message)
        if (validationError != null) {
            _responseState.value = QuotationResponseUiState.Error(validationError)
            return
        }

        respond(
            successMessage = "Observación enviada al agente.",
            action = { repository.observeQuotation(quotationId, message) },
            reloadObservationHistory = true
        )
    }

    fun cancelQuotation(quotationId: String) {
        respond(
            successMessage = "Cotización cancelada.",
            action = { repository.cancelQuotation(quotationId) }
        )
    }

    fun clearResponseState() {
        _responseState.value = QuotationResponseUiState.Idle
    }

    private fun respond(
        successMessage: String,
        reloadObservationHistory: Boolean = false,
        action: suspend () -> Quotation
    ) {
        if (_responseState.value is QuotationResponseUiState.Saving) return

        viewModelScope.launch {
            _responseState.value = QuotationResponseUiState.Saving
            try {
                val updatedQuotation = action()
                val observations = if (reloadObservationHistory) {
                    repository.getObservationHistory(updatedQuotation.id)
                } else {
                    _detailState.value.observations
                }

                _detailState.update {
                    it.copy(
                        quotation = updatedQuotation,
                        observations = observations,
                        errorMessage = null
                    )
                }
                updateQuotationInList(updatedQuotation)
                _responseState.value = QuotationResponseUiState.Success(
                    quotation = updatedQuotation,
                    message = successMessage
                )
            } catch (exception: Exception) {
                _responseState.value = QuotationResponseUiState.Error(
                    exception.localizedMessage ?: "No se pudo actualizar la cotización."
                )
            }
        }
    }

    private fun updateQuotationInList(updatedQuotation: Quotation) {
        val currentState = _quotationsState.value
        if (currentState is CustomerQuotationsUiState.Success) {
            _quotationsState.value = currentState.copy(
                quotations = currentState.quotations.map { quotation ->
                    if (quotation.id == updatedQuotation.id) updatedQuotation else quotation
                }
            )
        }
    }
}
