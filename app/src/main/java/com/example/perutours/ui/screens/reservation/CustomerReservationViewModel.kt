package com.example.perutours.ui.screens.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.PaymentReviewDataSource
import com.example.perutours.data.repository.ReservationDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CustomerReservationViewModel(
    private val repository: ReservationDataSource,
    private val paymentRepository: PaymentReviewDataSource
) : ViewModel() {

    private val _reservationsState =
        MutableStateFlow<CustomerReservationsUiState>(CustomerReservationsUiState.Loading)
    val reservationsState: StateFlow<CustomerReservationsUiState> =
        _reservationsState.asStateFlow()

    private val _detailState = MutableStateFlow(CustomerReservationDetailUiState())
    val detailState: StateFlow<CustomerReservationDetailUiState> = _detailState.asStateFlow()

    fun loadReservations() {
        viewModelScope.launch {
            _reservationsState.value = CustomerReservationsUiState.Loading
            _reservationsState.value = try {
                CustomerReservationsUiState.Success(
                    repository.getCurrentClientReservations()
                )
            } catch (_: Exception) {
                CustomerReservationsUiState.Error(
                    "No pudimos cargar tus reservas. Revisa tu conexión."
                )
            }
        }
    }

    fun loadReservation(reservationId: String) {
        if (reservationId.isBlank()) {
            _detailState.value = CustomerReservationDetailUiState(
                errorMessage = "No se recibió el identificador de la reserva."
            )
            return
        }

        viewModelScope.launch {
            _detailState.value = CustomerReservationDetailUiState(isLoading = true)
            _detailState.value = try {
                val reservation = repository.getCurrentClientReservationById(reservationId)
                    ?: throw IllegalStateException("No se encontró la reserva.")
                // HU07: el pago se consulta por separado para mostrar el
                // estado de la revisión y el motivo de rechazo; si falla,
                // la reserva se muestra igual.
                val payment = runCatching {
                    paymentRepository.getPaymentForReservation(reservationId)
                }.getOrNull()
                CustomerReservationDetailUiState(reservation = reservation, payment = payment)
            } catch (_: Exception) {
                CustomerReservationDetailUiState(
                    errorMessage = "No pudimos cargar esta reserva. Inténtalo nuevamente."
                )
            }
        }
    }
}
