package com.example.perutours.domain.payment

import java.math.BigDecimal
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class VoucherAssessmentTest {
    private val now = millis(2026, 11, 22, 14, 0)
    private val created = millis(2026, 11, 21, 10, 0)
    private val valid = VoucherOcrResult(VoucherAmount(BigDecimal("120.00"), "PEN"),
        "00123456", VoucherDate(2026, 11, 22), VoucherTime(13, 30))
    private fun assess(value: VoucherOcrResult, submitted: Long = 0) =
        VoucherValidator.assess(value, 120.0, "PEN", created, submitted, now)

    @Test fun matchingReceiptIsConsistentWithoutClaimingBankConfirmation() {
        assertEquals(VoucherAssessmentStatus.CONSISTENT, assess(valid).status)
    }

    @Test fun aYapeFrom2050IsBlockedEvenWhenItsAmountMatches() {
        val assessment = assess(valid.copy(transactionDate = VoucherDate(2050, 1, 1)))
        assertTrue(assessment.hasBlockingIssues)
        assertTrue(assessment.issues.any { it.message.contains("futuro") })
    }

    @Test fun futureHoursAreBlockedAndSmallClockDifferencesAreTolerated() {
        assertTrue(assess(valid.copy(transactionTime = VoucherTime(15, 0))).hasBlockingIssues)
        assertFalse(assess(valid.copy(transactionTime = VoucherTime(14, 3))).hasBlockingIssues)
    }

    @Test fun receiptCannotHaveBeenMadeAfterItsImageWasSubmitted() {
        assertTrue(assess(valid, millis(2026, 11, 22, 12, 0)).hasBlockingIssues)
    }

    @Test fun oldReceiptFromBeforeTheReservationIsBlocked() {
        assertTrue(assess(valid.copy(transactionDate = VoucherDate(2026, 11, 20))).hasBlockingIssues)
        assertFalse(assess(valid.copy(transactionDate = VoucherDate(2026, 11, 21))).hasBlockingIssues)
    }

    @Test fun amountAndCurrencyMismatchesAreBlocking() {
        assertTrue(assess(valid.copy(amount = VoucherAmount(BigDecimal("12.00"), "PEN"))).hasBlockingIssues)
        assertTrue(assess(valid.copy(amount = VoucherAmount(BigDecimal("120.00"), "USD"))).hasBlockingIssues)
    }

    @Test fun sameDayReceiptBeforeAcceptanceTimeIsBlocked() {
        val receipt = valid.copy(transactionDate = VoucherDate(2026, 11, 21), transactionTime = VoucherTime(8, 0))
        assertTrue(assess(receipt).hasBlockingIssues)
        assertFalse(assess(receipt.copy(transactionTime = VoucherTime(10, 1))).hasBlockingIssues)
    }

    @Test fun sameDayWithoutReadableTimeNeedsReviewRatherThanInventingAnHour() {
        val receipt = valid.copy(transactionDate = VoucherDate(2026, 11, 21), transactionTime = null)
        assertEquals(VoucherAssessmentStatus.NEEDS_REVIEW, assess(receipt).status)
        assertFalse(assess(receipt).hasBlockingIssues)
    }

    @Test fun unreadableFieldsArePendingRatherThanDeclaredCorrectOrForged() {
        val assessment = assess(valid.copy(transactionTime = null, operationNumber = null))
        assertEquals(VoucherAssessmentStatus.NEEDS_REVIEW, assessment.status)
        assertFalse(assessment.hasBlockingIssues)
    }

    @Test fun dateOnlyIsStillCheckedForTheFutureAndAgainstTheReservation() {
        assertTrue(assess(valid.copy(transactionDate = VoucherDate(2050, 1, 1), transactionTime = null)).hasBlockingIssues)
        assertFalse(assess(valid.copy(transactionTime = null)).hasBlockingIssues)
    }

    @Test fun impossibleDatesAndHoursCannotPassEvenWhenConstructedOutsideTheParser() {
        assertTrue(assess(valid.copy(transactionDate = VoucherDate(2026, 2, 30))).hasBlockingIssues)
        assertTrue(assess(valid.copy(transactionTime = VoucherTime(27, 80))).hasBlockingIssues)
    }

    @Test fun unknownReservationAmountAndNonFiniteMoneyNeverProduceSuccess() {
        assertEquals(VoucherAssessmentStatus.NEEDS_REVIEW,
            VoucherValidator.assess(valid, Double.NaN, "PEN", nowMillis = now).status)
        assertFalse(valid.amount!!.matches(Double.POSITIVE_INFINITY, "PEN"))
    }

    @Test fun datesUsePeruTimeEvenWhenThePhoneIsInAnotherTimeZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            assertEquals(VoucherAssessmentStatus.CONSISTENT, assess(valid).status)
        } finally { TimeZone.setDefault(original) }
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("America/Lima")).apply {
            clear(); set(year, month - 1, day, hour, minute)
        }.timeInMillis
}
