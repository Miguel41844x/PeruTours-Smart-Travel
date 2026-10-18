package com.example.perutours.ui.screens.reservation

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation

sealed interface CustomerReservationsUiState {
    data object Loading : CustomerReservationsUiState
    data class Success(val reservations: List<Reservation>) : CustomerReservationsUiState
    data class Error(val message: String) : CustomerReservationsUiState
}

data class CustomerReservationDetailUiState(
    val isLoading: Boolean = false,
    val reservation: Reservation? = null,
    // HU07: pago independiente asociado a la reserva; lleva el motivo
    // de rechazo cuando el agente devolvió el comprobante.
    val payment: Payment? = null,
    val errorMessage: String? = null
)
