package com.example.perutours.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.PaymentReviewDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentPaymentsUiState(
    val isLoading: Boolean = false,
    val payments: List<Payment> = emptyList(),
    val clientNames: Map<String, String> = emptyMap(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

data class AgentPaymentDetailUiState(
    val isLoading: Boolean = false,
    val payment: Payment? = null,
    val reservation: Reservation? = null,
    val clientName: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val resolutionMessage: String? = null
) {
    val isResolved: Boolean
        get() = payment?.status != null && payment.status != Payment.STATUS_PENDING
}

/**
 * HU07: panel del agente para revisar comprobantes. El agente asignado
 * lista los pagos 'Pendiente', aprueba o rechaza con motivo y las dos
 * colecciones (pago y reserva) se actualizan de forma atómica.
 */
class AgentPaymentViewModel(
    private val repository: PaymentReviewDataSource
) : ViewModel() {

    private val _paymentsState = MutableStateFlow(AgentPaymentsUiState())
    val paymentsState: StateFlow<AgentPaymentsUiState> = _paymentsState.asStateFlow()

    private val _detailState = MutableStateFlow(AgentPaymentDetailUiState())
    val detailState: StateFlow<AgentPaymentDetailUiState> = _detailState.asStateFlow()

    fun loadPendingPayments() {
        viewModelScope.launch {
            _paymentsState.update {
                it.copy(isLoading = true, errorMessage = null, infoMessage = null)
            }
            try {
                val payments = repository.getPendingPaymentsForAgent()
                val clientNames = payments
                    .distinctBy { payment -> payment.clientId }
                    .associate { payment ->
                        payment.clientId to (repository.getClientName(payment.clientId) ?: "")
                    }
                _paymentsState.update {
                    it.copy(
                        isLoading = false,
                        payments = payments,
                        clientNames = clientNames,
                        infoMessage = if (payments.isEmpty()) {
                            "No hay comprobantes pendientes por revisar."
                        } else {
                            null
                        }
                    )
                }
            } catch (_: Exception) {
                _paymentsState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No pudimos cargar los pagos pendientes. Revisa tu conexión."
                    )
                }
            }
        }
    }

    fun loadPaymentDetail(paymentId: String) {
        if (paymentId.isBlank()) {
            _detailState.update {
                it.copy(errorMessage = "No se recibió el identificador del pago.")
            }
            return
        }

        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val payment = repository.getPaymentForReservation(paymentId)
                    ?: throw IllegalStateException("No se encontró el pago.")
                val reservation = runCatching {
                    repository.getReservationForAgent(payment.reservationId)
                }.getOrNull()
                val clientName = runCatching {
                    repository.getClientName(payment.clientId)
                }.getOrNull()
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        payment = payment,
                        reservation = reservation,
                        clientName = clientName
                    )
                }
            } catch (_: Exception) {
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No pudimos cargar este pago. Inténtalo nuevamente."
                    )
                }
            }
        }
    }

    fun approvePayment() {
        val payment = _detailState.value.payment ?: return
        if (_detailState.value.isSubmitting || _detailState.value.isResolved) return

        viewModelScope.launch {
            _detailState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                repository.approvePayment(payment)
                _detailState.update {
                    it.copy(
                        isSubmitting = false,
                        payment = payment.copy(status = Payment.STATUS_APPROVED),
                        resolutionMessage = "Pago aprobado. La reserva quedó en estado Pagado."
                    )
                }
                loadPendingPayments()
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?: "No se pudo aprobar el pago. Inténtalo nuevamente."
                    )
                }
            }
        }
    }

    fun rejectPayment(rejectionReason: String) {
        val payment = _detailState.value.payment ?: return
        if (_detailState.value.isSubmitting || _detailState.value.isResolved) return

        viewModelScope.launch {
            _detailState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                repository.rejectPayment(payment, rejectionReason)
                _detailState.update {
                    it.copy(
                        isSubmitting = false,
                        payment = payment.copy(
                            status = Payment.STATUS_REJECTED,
                            rejectionReason = rejectionReason.trim()
                        ),
                        resolutionMessage = "Pago rechazado. El cliente podrá reenviar su comprobante."
                    )
                }
                loadPendingPayments()
            } catch (e: Exception) {
                _detailState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage
                            ?.takeIf(String::isNotBlank)
                            ?: "No se pudo rechazar el pago. Inténtalo nuevamente."
                    )
                }
            }
        }
    }
}
