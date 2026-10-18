package com.example.perutours.ui.screens.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.repository.PaymentReviewDataSource
import com.example.perutours.data.repository.ReservationDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

    private var reservationsJob: Job? = null

    fun loadReservations() {
        reservationsJob?.cancel()
        reservationsJob = viewModelScope.launch {
            _reservationsState.value = CustomerReservationsUiState.Loading
            try {
                repository.observeCurrentClientReservations().collect { reservations ->
                    _reservationsState.value = CustomerReservationsUiState.Success(reservations)
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _reservationsState.value = CustomerReservationsUiState.Error(
                    reservationErrorMessage(
                        error,
                        "No pudimos cargar tus reservas. Inténtalo nuevamente."
                    )
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
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                CustomerReservationDetailUiState(
                    errorMessage = reservationErrorMessage(
                        error,
                        "No pudimos cargar esta reserva. Inténtalo nuevamente."
                    )
                )
            }
        }
    }

    private fun reservationErrorMessage(error: Exception, fallback: String): String = when {
        error is IllegalStateException || error is IllegalAccessException ->
            error.localizedMessage?.takeIf(String::isNotBlank) ?: fallback
        error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para consultar esta reserva. Vuelve a iniciar sesión."
        error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "No hay conexión con el servidor. Revisa tu internet e inténtalo nuevamente."
        else -> fallback
    }
}
