package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.Reservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HU07: Pruebas de las reglas de negocio del flujo de pago
 * (Confirmada → Pago enviado → Pagado | Pago rechazado).
 */
class PaymentValidatorTest {

    @Test
    fun `solo los metodos permitidos son validos`() {
        assertTrue(PaymentValidator.isValidMethod("Yape"))
        assertTrue(PaymentValidator.isValidMethod("Plin"))
        assertTrue(PaymentValidator.isValidMethod("Transferencia BCP"))
        assertFalse(PaymentValidator.isValidMethod("Efectivo"))
        assertFalse(PaymentValidator.isValidMethod(""))
    }

    @Test
    fun `el metodo por defecto del worker no es un metodo valido`() {
        // Regresión: el valor heredado "Transferencia / Yape" no debe aceptarse.
        assertFalse(PaymentValidator.isValidMethod("Transferencia / Yape"))
    }

    @Test
    fun `solo una reserva confirmada o con pago rechazado permite enviar comprobante`() {
        assertTrue(PaymentValidator.canClientSubmit(Reservation.STATUS_CONFIRMED))
        assertTrue(PaymentValidator.canClientSubmit(Reservation.STATUS_PAYMENT_REJECTED))
        assertFalse(PaymentValidator.canClientSubmit(Reservation.STATUS_PAYMENT_PENDING))
        assertFalse(PaymentValidator.canClientSubmit(Reservation.STATUS_PAID))
    }

    @Test
    fun `solo el agente puede revisar pagos pendientes`() {
        assertTrue(PaymentValidator.canAgentReview(Payment.STATUS_PENDING))
        assertFalse(PaymentValidator.canAgentReview(Payment.STATUS_APPROVED))
        assertFalse(PaymentValidator.canAgentReview(Payment.STATUS_REJECTED))
    }

    @Test
    fun `un pago valido cumple metodo importe moneda y voucher`() {
        val payment = Payment(
            id = "reserva_1",
            reservationId = "reserva_1",
            clientId = "cliente_1",
            agentId = "agente_1",
            method = "Yape",
            amount = 1500.0,
            currency = "PEN",
            voucherUrl = "https://firebasestorage.googleapis.com/voucher.jpg",
            status = Payment.STATUS_PENDING,
            submittedAtMillis = System.currentTimeMillis()
        )
        assertTrue(PaymentValidator.isValidSubmittedPayment(payment))
    }

    @Test
    fun `un pago con importe en cero o voucher local no es valido`() {
        val base = Payment(
            method = "Yape",
            amount = 1500.0,
            currency = "PEN",
            voucherUrl = "https://firebasestorage.googleapis.com/voucher.jpg",
            status = Payment.STATUS_PENDING
        )
        assertFalse(PaymentValidator.isValidSubmittedPayment(base.copy(amount = 0.0)))
        assertFalse(
            PaymentValidator.isValidSubmittedPayment(base.copy(voucherUrl = "file:///local/voucher.jpg"))
        )
        assertFalse(
            PaymentValidator.isValidSubmittedPayment(base.copy(status = Payment.STATUS_APPROVED))
        )
    }

    @Test
    fun `el importe debe coincidir con el total de la reserva`() {
        assertTrue(PaymentValidator.amountMatchesReservation(1500.0, 1500.0))
        assertTrue(PaymentValidator.amountMatchesReservation(1500.0, 1500.005))
        assertFalse(PaymentValidator.amountMatchesReservation(1500.0, 1400.0))
    }

    @Test
    fun `el motivo de rechazo requiere al menos 10 caracteres`() {
        assertTrue(PaymentValidator.isValidRejectionReason("Comprobante ilegible, sube otra foto."))
        assertFalse(PaymentValidator.isValidRejectionReason("Malo"))
        assertFalse(PaymentValidator.isValidRejectionReason("   "))
    }

    @Test
    fun `solo imagenes jpg jpeg png webp menores a 10 MB son validas`() {
        assertTrue(PaymentValidator.isValidImageExtension("jpg"))
        assertTrue(PaymentValidator.isValidImageExtension("PNG"))
        assertTrue(PaymentValidator.isValidImageExtension("webp"))
        assertFalse(PaymentValidator.isValidImageExtension("gif"))
        assertFalse(PaymentValidator.isValidImageExtension("pdf"))

        assertTrue(PaymentValidator.isValidVoucherSize(1024L))
        assertTrue(PaymentValidator.isValidVoucherSize(PaymentValidator.MAX_VOUCHER_BYTES))
        assertFalse(PaymentValidator.isValidVoucherSize(0L))
        assertFalse(PaymentValidator.isValidVoucherSize(PaymentValidator.MAX_VOUCHER_BYTES + 1))
    }

    @Test
    fun `la reserva conserva los cuatro estados del flujo de pago`() {
        assertEquals("Confirmada", Reservation.STATUS_CONFIRMED)
        assertEquals("Pago enviado", Reservation.STATUS_PAYMENT_PENDING)
        assertEquals("Pagado", Reservation.STATUS_PAID)
        assertEquals("Pago rechazado", Reservation.STATUS_PAYMENT_REJECTED)
    }
}
