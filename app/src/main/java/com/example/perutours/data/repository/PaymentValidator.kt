package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import kotlin.math.abs

/**
 * HU07: Reglas de negocio puras del flujo de pago, sin dependencias de
 * Firebase, para poder probarlas con pruebas unitarias.
 *
 * Flujo: Confirmada → Pago enviado → Pagado | Pago rechazado → Pago enviado…
 */
object PaymentValidator {

    val ALLOWED_METHODS = listOf("Yape", "Plin", "Transferencia BCP")

    val ALLOWED_CURRENCIES = listOf("PEN", "USD")

    const val MIN_REJECTION_LENGTH = 10

    const val MAX_VOUCHER_BYTES = 10L * 1024L * 1024L

    val ALLOWED_IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

    fun isValidMethod(method: String): Boolean = method in ALLOWED_METHODS

    fun isValidCurrency(currency: String): Boolean = currency in ALLOWED_CURRENCIES

    fun isValidRejectionReason(reason: String): Boolean =
        reason.trim().length >= MIN_REJECTION_LENGTH

    /** Solo una reserva Confirmada o con pago rechazado permite (re)enviar comprobante. */
    fun canClientSubmit(reservationStatus: String): Boolean =
        reservationStatus == Reservation.STATUS_CONFIRMED ||
            reservationStatus == Reservation.STATUS_PAYMENT_REJECTED

    /** Solo el agente puede revisar pagos que aún están Pendientes. */
    fun canAgentReview(paymentStatus: String): Boolean =
        paymentStatus == Payment.STATUS_PENDING

    fun isValidSubmittedPayment(payment: Payment): Boolean =
        isValidMethod(payment.method) &&
            payment.amount > 0.0 &&
            isValidCurrency(payment.currency) &&
            payment.voucherUrl.startsWith("https://") &&
            payment.status == Payment.STATUS_PENDING

    /** El importe reportado debe coincidir con el total de la reserva (±0.01). */
    fun amountMatchesReservation(paymentAmount: Double, reservationAmount: Double): Boolean =
        abs(paymentAmount - reservationAmount) < 0.01

    fun isValidImageExtension(extension: String): Boolean =
        extension.lowercase() in ALLOWED_IMAGE_EXTENSIONS

    fun isValidVoucherSize(bytes: Long): Boolean =
        bytes in 1..MAX_VOUCHER_BYTES
}
