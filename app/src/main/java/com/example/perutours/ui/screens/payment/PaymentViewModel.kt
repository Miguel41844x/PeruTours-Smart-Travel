package com.example.perutours.ui.screens.payment

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.PaymentRepository
import com.example.perutours.data.repository.PaymentUploadState
import com.example.perutours.data.repository.PaymentValidator
import com.example.perutours.data.repository.ReservationDataSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PaymentUiState(
    val voucherUri: Uri? = null,
    val paymentMethod: String? = null,
    val isPreparing: Boolean = false,
    val uploadState: PaymentUploadState = PaymentUploadState.Idle,
    // HU07: el importe y la moneda autoritativos se leen de la reserva
    // en Firestore; el total recibido por navegación es solo informativo.
    val reservationAmount: Double? = null,
    val reservationCurrency: String = "PEN",
    val reservationStatus: String = "",
    val isLoadingReservation: Boolean = true,
    val errorMessage: String? = null
) {
    val paymentMethods: List<String>
        get() = PaymentValidator.ALLOWED_METHODS

    val canSubmit: Boolean
        get() = voucherUri != null
            && paymentMethod != null
            && !isPreparing
            && !isLoadingReservation
            && (uploadState is PaymentUploadState.Idle ||
                uploadState is PaymentUploadState.Error)
            && PaymentValidator.canClientSubmit(reservationStatus)
}

/**
 * HU07: el cliente elige un método de pago válido (Yape, Plin o
 * Transferencia BCP), adjunta el comprobante y sigue la subida en
 * tiempo real gracias al WorkInfo del Worker.
 */
class PaymentViewModel(
    private val paymentRepository: PaymentRepository,
    private val reservationRepository: ReservationDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private var uploadJob: Job? = null

    /**
     * Carga la reserva desde Firestore para tomar el importe y la
     * moneda reales y para comprobar que admite un nuevo comprobante
     * (solo 'Confirmada' o 'Pago rechazado').
     */
    fun loadReservation(reservationId: String) {
        if (reservationId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoadingReservation = false,
                    errorMessage = "No se recibió el identificador de la reserva."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReservation = true, errorMessage = null) }
            try {
                val reservation =
                    reservationRepository.getCurrentClientReservationById(reservationId)
                        ?: throw IllegalStateException("No se encontró la reserva.")
                _uiState.update {
                    it.copy(
                        isLoadingReservation = false,
                        reservationAmount = reservation.totalAmount,
                        reservationCurrency = reservation.currency,
                        reservationStatus = reservation.status
                    )
                }
                if (!PaymentValidator.canClientSubmit(reservation.status)) {
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = "Esta reserva ya no admite un nuevo comprobante."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingReservation = false,
                        errorMessage = e.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?: "No pudimos cargar la reserva. Inténtalo nuevamente."
                    )
                }
            }
        }
    }

    fun onVoucherCaptured(uri: Uri) {
        _uiState.update {
            it.copy(voucherUri = uri, uploadState = PaymentUploadState.Idle, errorMessage = null)
        }
    }

    fun onPaymentMethodChange(method: String) {
        if (PaymentValidator.isValidMethod(method)) {
            _uiState.update { it.copy(paymentMethod = method, errorMessage = null) }
        }
    }

    fun submitPayment(quotationId: String, reservationId: String) {
        if (quotationId.isBlank() || reservationId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "No se pudo identificar la reserva.") }
            return
        }

        val state = _uiState.value
        val method = state.paymentMethod
        if (method == null || !PaymentValidator.isValidMethod(method)) {
            _uiState.update { it.copy(errorMessage = "Selecciona un método de pago.") }
            return
        }

        val uri = state.voucherUri
        if (uri == null) {
            _uiState.update {
                it.copy(errorMessage = "Por favor toma una foto o selecciona tu comprobante.")
            }
            return
        }

        if (!PaymentValidator.canClientSubmit(state.reservationStatus)) {
            _uiState.update {
                it.copy(errorMessage = "Esta reserva no admite un nuevo comprobante.")
            }
            return
        }

        _uiState.update {
            it.copy(isPreparing = true, errorMessage = null, uploadState = PaymentUploadState.Idle)
        }

        viewModelScope.launch {
            try {
                // 1. Guardar copia local (sobrevive a cierre de app)
                val localPath = paymentRepository.saveVoucherLocally(uri)

                // 2. Encolar en WorkManager (diferido si no hay conexión)
                val workId = paymentRepository.enqueuePaymentUpload(
                    quotationId = quotationId,
                    reservationId = reservationId,
                    localImagePath = localPath,
                    paymentMethod = method
                )

                _uiState.update { it.copy(isPreparing = false) }

                // 3. Seguir el progreso real de la subida
                trackUpload(workId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPreparing = false,
                        errorMessage = e.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?: "No se pudo preparar el comprobante. Inténtalo nuevamente."
                    )
                }
            }
        }
    }

    /** Reintenta la subida tras un error definitivo, reutilizando la imagen. */
    fun retryUpload(quotationId: String, reservationId: String) {
        _uiState.update { it.copy(uploadState = PaymentUploadState.Idle, errorMessage = null) }
        submitPayment(quotationId, reservationId)
    }

    private fun trackUpload(workId: UUID) {
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            paymentRepository.observeUpload(workId).collect { uploadState ->
                _uiState.update { it.copy(uploadState = uploadState) }
            }
        }
    }
}
