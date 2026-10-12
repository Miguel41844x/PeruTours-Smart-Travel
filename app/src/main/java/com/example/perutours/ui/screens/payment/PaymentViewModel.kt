package com.example.perutours.ui.screens.payment

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PaymentUiState(
    val voucherUri: Uri? = null,
    val paymentMethod: String = "Yape / Plin / Transferencia BCP",
    val isSubmitting: Boolean = false,
    val uploadEnqueued: Boolean = false,
    val errorMessage: String? = null
)

class PaymentViewModel(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun onVoucherCaptured(uri: Uri) {
        _uiState.update {
            it.copy(voucherUri = uri, uploadEnqueued = false, errorMessage = null)
        }
    }

    fun onPaymentMethodChange(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun submitPayment(quotationId: String, reservationId: String) {
        if (quotationId.isBlank() || reservationId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No se pudo identificar la reserva.") }
            return
        }

        val uri = _uiState.value.voucherUri
        if (uri == null) {
            _uiState.update { it.copy(errorMessage = "Por favor toma una foto o selecciona tu comprobante.") }
            return
        }

        val paymentMethod = _uiState.value.paymentMethod
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Guardar localmente
                val localPath = paymentRepository.saveVoucherLocally(uri)

                // 2. Encolar en WorkManager (diferido si no hay conexión)
                paymentRepository.enqueuePaymentUpload(
                    quotationId = quotationId,
                    reservationId = reservationId,
                    localImagePath = localPath,
                    paymentMethod = paymentMethod
                )

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        uploadEnqueued = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?: "No se pudo preparar el comprobante. Inténtalo nuevamente."
                    )
                }
            }
        }
    }
}
