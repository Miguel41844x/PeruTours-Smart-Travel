package com.example.perutours.ui.screens.agent

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.ocr.VoucherTextReader
import com.example.perutours.data.repository.PaymentReviewDataSource
import com.example.perutours.data.repository.PaymentValidator
import com.example.perutours.domain.AgentPaymentQuery
import com.example.perutours.domain.payment.VoucherValidator
import com.example.perutours.ui.screens.payment.VoucherOcrState
import com.example.perutours.ui.screens.payment.readForReview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    val voucherOcr: VoucherOcrState = VoucherOcrState.Idle,
    val bankReceiptConfirmed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val resolutionMessage: String? = null
) {
    val isResolved: Boolean
        get() = payment?.status != null && payment.status != Payment.STATUS_PENDING

    val matchesReservation: Boolean
        get() {
            val pay = payment ?: return false
            val booking = reservation ?: return false
            return pay.reservationId == booking.id && pay.quotationId == booking.quotationId &&
                pay.clientId == booking.clientId && pay.agentId == booking.agentId &&
                booking.paymentId == pay.id && booking.status == Reservation.STATUS_PAYMENT_PENDING &&
                pay.currency == booking.currency && PaymentValidator.isValidSubmittedPayment(pay) &&
                booking.totalAmount.isFinite() &&
                PaymentValidator.amountMatchesReservation(pay.amount, booking.totalAmount)
        }

    val voucherAssessment
        get() = (voucherOcr as? VoucherOcrState.Read)?.let {
            VoucherValidator.assess(it.result, reservation?.totalAmount, reservation?.currency.orEmpty(),
                reservation?.createdAtMillis ?: 0L, payment?.submittedAtMillis ?: 0L)
        }

    val canApprove: Boolean
        get() = !isLoading && !isSubmitting && !isResolved && matchesReservation &&
            bankReceiptConfirmed && voucherAssessment?.hasBlockingIssues != true &&
            voucherOcr != VoucherOcrState.Idle && voucherOcr != VoucherOcrState.Reading
}

/**
 * HU07: panel del agente para revisar comprobantes. El agente asignado
 * lista los pagos 'Pendiente', aprueba o rechaza con motivo y las dos
 * colecciones (pago y reserva) se actualizan de forma atómica.
 */
class AgentPaymentViewModel(
    private val repository: PaymentReviewDataSource,
    private val voucherTextReader: VoucherTextReader
) : ViewModel() {

    private val _paymentsState = MutableStateFlow(AgentPaymentsUiState())
    val paymentsState: StateFlow<AgentPaymentsUiState> = _paymentsState.asStateFlow()
    private val _query = MutableStateFlow(AgentPaymentQuery())
    val query = _query.asStateFlow()
    fun searchPayments(value: String) {
        _query.value = _query.value.copy(text = value.take(120), visibleLimit = 20)
    }
    fun filterMethod(method: String?) {
        _query.value = _query.value.copy(method = method, visibleLimit = 20)
    }
    fun loadMorePayments() {
        _query.value = _query.value.copy(visibleLimit = _query.value.visibleLimit + 20)
    }

    private val _detailState = MutableStateFlow(AgentPaymentDetailUiState())
    val detailState: StateFlow<AgentPaymentDetailUiState> = _detailState.asStateFlow()
    private var detailLoadJob: Job? = null
    private var voucherOcrJob: Job? = null

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
            } catch (error: Exception) {
                if (error is CancellationException) throw error
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
        detailLoadJob?.cancel()
        voucherOcrJob?.cancel()
        _detailState.value = AgentPaymentDetailUiState(isLoading = paymentId.isNotBlank())
        if (paymentId.isBlank()) {
            _detailState.update {
                it.copy(errorMessage = "No se recibió el identificador del pago.")
            }
            return
        }

        detailLoadJob = viewModelScope.launch {
            try {
                val payment = repository.getPaymentById(paymentId)
                    ?: throw IllegalStateException("No se encontró el pago.")
                val reservation = runCatching {
                    repository.getReservationForAgent(payment.reservationId)
                }.getOrNull()
                val clientName = runCatching {
                    repository.getClientName(payment.clientId)
                }.getOrNull()
                ensureActive()
                _detailState.update {
                    it.copy(
                        isLoading = false,
                        payment = payment,
                        reservation = reservation,
                        clientName = clientName
                    )
                }
                readVoucher()
            } catch (cancelled: CancellationException) {
                throw cancelled
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

    fun readVoucher() {
        val state = _detailState.value
        val url = state.payment?.voucherUrl?.takeIf(String::isNotBlank) ?: return
        if (state.isLoading || state.voucherOcr == VoucherOcrState.Reading) return

        _detailState.update { it.copy(voucherOcr = VoucherOcrState.Reading, bankReceiptConfirmed = false) }
        voucherOcrJob = viewModelScope.launch {
            val reading = voucherTextReader.readForReview(Uri.parse(url))
            _detailState.update { it.copy(voucherOcr = reading) }
        }
    }

    fun approvePayment() {
        val payment = _detailState.value.payment ?: return
        if (!_detailState.value.canApprove) {
            _detailState.update { it.copy(errorMessage = "Revisa los datos del comprobante y confirma el abono en la cuenta.") }
            return
        }

        _detailState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
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
                if (e is CancellationException) throw e
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
        if (!PaymentValidator.isValidRejectionReason(rejectionReason)) {
            _detailState.update { it.copy(errorMessage = "Escribe un motivo de entre 10 y 500 caracteres.") }
            return
        }

        _detailState.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
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
                if (e is CancellationException) throw e
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

    fun confirmBankReceipt(confirmed: Boolean) {
        if (_detailState.value.isSubmitting || _detailState.value.isResolved) return
        _detailState.update { it.copy(bankReceiptConfirmed = confirmed, errorMessage = null) }
    }
}
