package com.example.perutours.domain.payment

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

enum class VoucherAssessmentStatus { CONSISTENT, INCONSISTENT, NEEDS_REVIEW }
data class VoucherIssue(val message: String, val blocking: Boolean, val highlighted: Boolean = blocking)
data class VoucherAssessment(val issues: List<VoucherIssue>) {
    val hasBlockingIssues: Boolean get() = issues.any(VoucherIssue::blocking)
    val status: VoucherAssessmentStatus get() = when {
        hasBlockingIssues -> VoucherAssessmentStatus.INCONSISTENT
        issues.isNotEmpty() -> VoucherAssessmentStatus.NEEDS_REVIEW
        else -> VoucherAssessmentStatus.CONSISTENT
    }
}

/** Comprueba coherencia de los datos leídos; no certifica un abono bancario. */
object VoucherValidator {
    private val paymentZone = TimeZone.getTimeZone("America/Lima")
    private val clockToleranceMillis = TimeUnit.MINUTES.toMillis(5)

    fun assess(
        reading: VoucherOcrResult,
        expectedAmount: Double?,
        expectedCurrency: String,
        reservationCreatedAtMillis: Long = 0,
        submittedAtMillis: Long = 0,
        nowMillis: Long = System.currentTimeMillis()
    ): VoucherAssessment {
        val issues = mutableListOf<VoucherIssue>()
        fun issue(message: String, blocking: Boolean = false, highlighted: Boolean = blocking) {
            issues += VoucherIssue(message, blocking, highlighted)
        }
        val expectedValid = expectedAmount != null && expectedAmount.isFinite() && expectedAmount > 0 &&
            expectedCurrency in setOf("PEN", "USD")
        if (!expectedValid) issue("No se ha podido comprobar el importe de la reserva.", highlighted = true)
        if (reservationCreatedAtMillis <= 0) issue("No consta la fecha de creación de la reserva: requiere revisión.", highlighted = true)
        val amount = reading.amount
        if (amount == null) issue("Importe o moneda sin leer: requiere comprobación manual.")
        else if (amount.value.signum() <= 0) issue("El importe del comprobante debe ser mayor que cero.", true)
        else if (expectedValid && !amount.matches(expectedAmount!!, expectedCurrency)) {
            issue("El importe o la moneda no coincide con la reserva.", true)
        }
        val operation = reading.operationNumber
        if (operation.isNullOrBlank()) issue("Número de operación sin leer: requiere comprobación manual.")
        else if (!operation.uppercase(Locale.ROOT).matches(Regex("[A-Z0-9]{4,32}"))) {
            issue("El número de operación no tiene un formato reconocible.", true)
        }
        val date = reading.transactionDate
        val time = reading.transactionTime
        if (date == null) issue("Fecha sin leer: requiere comprobación manual.")
        if (time == null) issue("Hora sin leer: requiere comprobación manual.")
        val validTime = time == null || (time.hour in 0..23 && time.minute in 0..59 &&
            (time.second == null || time.second in 0..59))
        if (!validTime) issue("La hora del comprobante no es válida.", true)
        if (date != null) {
            val dateMillis = calendarMillis(date, null)
            if (dateMillis == null) issue("La fecha del comprobante no es válida.", true)
            else {
                // Nunca aceptar una operación posterior al envío de su imagen.
                val reference = if (submittedAtMillis > 0) minOf(nowMillis, submittedAtMillis) else nowMillis
                val future = if (time != null && validTime) {
                    calendarMillis(date, time)?.let { it > reference + clockToleranceMillis } == true
                } else dateMillis > startOfDay(reference)
                if (future) issue(if (submittedAtMillis > 0 && submittedAtMillis < nowMillis)
                    "La fecha u hora del comprobante es posterior a su envío."
                    else "La fecha u hora del comprobante está en el futuro.", true)
                val beforeReservation = if (time != null && validTime) {
                    calendarMillis(date, time)?.let { it + clockToleranceMillis < reservationCreatedAtMillis } == true
                } else dateMillis < startOfDay(reservationCreatedAtMillis)
                if (reservationCreatedAtMillis > 0 && beforeReservation) {
                    issue("El comprobante es anterior a la creación de esta reserva.", true)
                }
            }
        }
        return VoucherAssessment(issues)
    }

    private fun calendarMillis(date: VoucherDate, time: VoucherTime?): Long? {
        if (date.year !in 1..9999 || date.month !in 1..12 || date.day !in 1..31) return null
        return try {
            GregorianCalendar(paymentZone, Locale.ROOT).apply {
                isLenient = false
                clear()
                set(date.year, date.month - 1, date.day, time?.hour ?: 0,
                    time?.minute ?: 0, time?.second ?: 0)
            }.timeInMillis
        } catch (_: IllegalArgumentException) { null }
    }

    private fun startOfDay(value: Long): Long = Calendar.getInstance(paymentZone).apply {
        timeInMillis = value
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
