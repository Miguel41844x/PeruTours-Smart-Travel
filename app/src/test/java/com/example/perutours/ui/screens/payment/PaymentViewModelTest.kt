package com.example.perutours.ui.screens.payment

import android.net.Uri
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.repository.PaymentRepository
import com.example.perutours.data.repository.PaymentUploadState
import com.example.perutours.data.repository.ReservationDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * HU07: pruebas del ViewModel de pago: selección de método, monto
 * autoritativo desde la reserva y seguimiento del estado de subida.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private lateinit var paymentRepository: PaymentRepository
    private lateinit var reservationRepository: ReservationDataSource
    private lateinit var viewModel: PaymentViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val voucherUri = mockk<Uri>()

    private fun reservation(
        status: String = Reservation.STATUS_CONFIRMED,
        totalAmount: Double = 1500.0
    ) = Reservation(
        id = "res_1",
        quotationId = "res_1",
        clientId = "cliente_1",
        agentId = "agente_1",
        destination = "Cusco",
        originCity = "Lima",
        totalAmount = totalAmount,
        currency = "PEN",
        status = status
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        paymentRepository = mockk(relaxed = true)
        reservationRepository = mockk(relaxed = true)
        viewModel = PaymentViewModel(paymentRepository, reservationRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadReservation toma importe y moneda de la reserva en Firestore`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation(totalAmount = 1890.5)

        viewModel.loadReservation("res_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1890.5, state.reservationAmount!!, 0.01)
        assertEquals("PEN", state.reservationCurrency)
        assertEquals(Reservation.STATUS_CONFIRMED, state.reservationStatus)
        assertFalse(state.isLoadingReservation)
    }

    @Test
    fun `loadReservation con reserva ya pagada bloquea un nuevo comprobante`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation(status = Reservation.STATUS_PAID)

        viewModel.loadReservation("res_1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.canSubmit)
        assertTrue(state.errorMessage != null)
    }

    @Test
    fun `loadReservation con pago rechazado permite reenviar el comprobante`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation(status = Reservation.STATUS_PAYMENT_REJECTED)

        viewModel.loadReservation("res_1")
        viewModel.onVoucherCaptured(voucherUri)
        viewModel.onPaymentMethodChange("Yape")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `onPaymentMethodChange ignora metodos fuera del catalogo`() {
        viewModel.onPaymentMethodChange("Efectivo")
        assertNull(viewModel.uiState.value.paymentMethod)

        viewModel.onPaymentMethodChange("Transferencia / Yape")
        assertNull(viewModel.uiState.value.paymentMethod)

        viewModel.onPaymentMethodChange("Plin")
        assertEquals("Plin", viewModel.uiState.value.paymentMethod)
    }

    @Test
    fun `submitPayment sin metodo seleccionado muestra error y no encola`() {
        viewModel.onVoucherCaptured(voucherUri)

        viewModel.submitPayment("res_1", "res_1")

        assertTrue(viewModel.uiState.value.errorMessage != null)
        verify(exactly = 0) { paymentRepository.enqueuePaymentUpload(any(), any(), any(), any()) }
    }

    @Test
    fun `submitPayment sin comprobante muestra error y no encola`() {
        viewModel.onPaymentMethodChange("Yape")

        viewModel.submitPayment("res_1", "res_1")

        assertTrue(viewModel.uiState.value.errorMessage != null)
        verify(exactly = 0) { paymentRepository.enqueuePaymentUpload(any(), any(), any(), any()) }
    }

    @Test
    fun `submitPayment valida contra el estado de la reserva antes de encolar`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation(status = Reservation.STATUS_PAYMENT_PENDING)

        viewModel.loadReservation("res_1")
        advanceUntilIdle()
        viewModel.onVoucherCaptured(voucherUri)
        viewModel.onPaymentMethodChange("Yape")
        viewModel.submitPayment("res_1", "res_1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage != null)
        verify(exactly = 0) { paymentRepository.enqueuePaymentUpload(any(), any(), any(), any()) }
    }

    @Test
    fun `submitPayment encola con el metodo elegido y sigue el progreso`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation()
        val workId = UUID.randomUUID()
        val methodSlot = slot<String>()
        coEvery { paymentRepository.saveVoucherLocally(any()) } returns "/tmp/voucher.jpg"
        every {
            paymentRepository.enqueuePaymentUpload(any(), any(), any(), capture(methodSlot))
        } returns workId
        every {
            paymentRepository.observeUpload(workId)
        } returns flowOf(
            PaymentUploadState.Enqueued,
            PaymentUploadState.Uploading,
            PaymentUploadState.Uploaded
        )

        viewModel.loadReservation("res_1")
        advanceUntilIdle()
        viewModel.onVoucherCaptured(voucherUri)
        viewModel.onPaymentMethodChange("Transferencia BCP")
        viewModel.submitPayment("res_1", "res_1")
        advanceUntilIdle()

        assertEquals("Transferencia BCP", methodSlot.captured)
        coVerify(exactly = 1) { paymentRepository.saveVoucherLocally(voucherUri) }
        assertEquals(PaymentUploadState.Uploaded, viewModel.uiState.value.uploadState)
    }

    @Test
    fun `retryUpload vuelve a encolar la subida tras un error`() = runTest {
        coEvery {
            reservationRepository.getCurrentClientReservationById("res_1")
        } returns reservation()
        val workId = UUID.randomUUID()
        coEvery { paymentRepository.saveVoucherLocally(any()) } returns "/tmp/voucher.jpg"
        every {
            paymentRepository.enqueuePaymentUpload(any(), any(), any(), any())
        } returns workId
        every {
            paymentRepository.observeUpload(workId)
        } returns flowOf(PaymentUploadState.Error("Sin conexión"))

        viewModel.loadReservation("res_1")
        advanceUntilIdle()
        viewModel.onVoucherCaptured(voucherUri)
        viewModel.onPaymentMethodChange("Yape")
        viewModel.submitPayment("res_1", "res_1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.uploadState is PaymentUploadState.Error)

        viewModel.retryUpload("res_1", "res_1")
        advanceUntilIdle()

        coVerify(exactly = 2) { paymentRepository.saveVoucherLocally(any()) }
        verify(exactly = 2) { paymentRepository.enqueuePaymentUpload(any(), any(), any(), any()) }
    }
}
