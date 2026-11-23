package com.example.perutours.ui.screens.agent

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import com.example.perutours.domain.payment.*
import com.example.perutours.ui.screens.payment.VoucherOcrState
import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class AgentPaymentReviewStateTest {
    private val now = System.currentTimeMillis()
    private val booking = Reservation(id = "res", quotationId = "quote", clientId = "client",
        agentId = "agent", paymentId = "pay", status = Reservation.STATUS_PAYMENT_PENDING,
        totalAmount = 120.0, createdAtMillis = 0)
    private val payment = Payment(id = "pay", quotationId = "quote", reservationId = "res",
        clientId = "client", agentId = "agent", amount = 120.0, method = "Yape",
        voucherUrl = "https://example.test/voucher.jpg", submittedAtMillis = now)
    private val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
        timeInMillis = now - 600_000L
    }
    private val reading = VoucherOcrResult(VoucherAmount(BigDecimal("120.00"), "PEN"), "00123456",
        VoucherDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)),
        VoucherTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)))
    private val state = AgentPaymentDetailUiState(payment = payment, reservation = booking,
        voucherOcr = VoucherOcrState.Read(reading))

    @Test fun coherentReceiptDoesNotAutomaticallyApproveThePayment() {
        assertTrue(state.matchesReservation)
        assertFalse(state.canApprove)
        assertTrue(state.copy(bankReceiptConfirmed = true).canApprove)
    }

    @Test fun aFutureOrDifferentAmountCannotBeApprovedByCheckingTheBox() {
        assertFalse(state.copy(bankReceiptConfirmed = true, voucherOcr = VoucherOcrState.Read(
            reading.copy(transactionDate = VoucherDate(2050, 1, 1)))).canApprove)
        assertFalse(state.copy(bankReceiptConfirmed = true, voucherOcr = VoucherOcrState.Read(
            reading.copy(amount = VoucherAmount(BigDecimal("12"), "PEN")))).canApprove)
    }

    @Test fun unreadableReceiptRequiresExplicitManualConfirmationAndReadingMustFinish() {
        assertFalse(state.copy(voucherOcr = VoucherOcrState.Unavailable).canApprove)
        assertTrue(state.copy(voucherOcr = VoucherOcrState.Unavailable, bankReceiptConfirmed = true).canApprove)
        assertFalse(state.copy(voucherOcr = VoucherOcrState.Reading, bankReceiptConfirmed = true).canApprove)
    }

    @Test fun imageWithoutPaymentDataCannotBeApprovedEvenWithBankConfirmation() {
        assertFalse(state.copy(voucherOcr = VoucherOcrState.NoData,
            bankReceiptConfirmed = true).canApprove)
        assertFalse(state.copy(voucherOcr = VoucherOcrState.Read(VoucherOcrResult(null, null)),
            bankReceiptConfirmed = true).canApprove)
        assertTrue(state.copy(voucherOcr = VoucherOcrState.Read(reading.copy(
            operationNumber = null, transactionTime = null)), bankReceiptConfirmed = true).canApprove)
    }

    @Test fun aSupersededAttemptOrUnrelatedReservationCannotBeApproved() {
        assertFalse(state.copy(bankReceiptConfirmed = true,
            reservation = booking.copy(paymentId = "newer-attempt")).canApprove)
        assertFalse(state.copy(bankReceiptConfirmed = true,
            reservation = booking.copy(clientId = "other-client")).canApprove)
        assertFalse(state.copy(bankReceiptConfirmed = true,
            payment = payment.copy(amount = Double.POSITIVE_INFINITY)).canApprove)
    }
}
