package com.example.perutours.domain.payment

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoucherTextLayoutTest {
    @Test
    fun associatesOperationAcrossColumnsRegardlessOfRecognitionOrder() {
        val result = parse(
            line("00123456", 600, 800, 130),
            line("Código de seguridad", 50, 500, 240),
            line("8456", 600, 500, 100),
            line("Nro. de operación", 50, 800, 240),
            line("21 nov. 2026", 50, 400, 180),
            line("S/ 12", 50, 300, 150)
        )
        assertEquals("00123456", result.operationNumber)
        assertEquals(VoucherAmount(BigDecimal("12.00"), "PEN"), result.amount)
    }

    @Test
    fun supportsAlphanumericBankCodesAndValuesBelowTheirLabel() {
        val result = parse(
            line("6ABC2F9B6E5", 50, 142, 220),
            line("Número de operación:", 50, 100, 280)
        )
        assertEquals("6ABC2F9B6E5", result.operationNumber)
    }

    @Test
    fun supportsOperationOnTheSameRecognizedLine() {
        assertEquals("731546", parse(line("N.º de operación: 731546", 50, 100, 400)).operationNumber)
        assertEquals("731546", parse(line("№ de operación: 731546", 50, 100, 400)).operationNumber)
    }

    @Test
    fun leavesAmbiguousOrDistantOperationsForManualReview() {
        assertNull(parse(
            line("Número de operación", 50, 100, 250),
            line("12345678", 400, 100, 130),
            line("87654321", 600, 100, 130)
        ).operationNumber)
        assertNull(parse(
            line("Número de operación", 50, 100, 250),
            line("12345678", 50, 400, 130)
        ).operationNumber)
    }

    @Test
    fun doesNotUseASecurityCodeAnAccountOrADateAsAnOperation() {
        assertNull(parse(
            line("Código de seguridad", 50, 100, 250),
            line("123456", 400, 100, 130),
            line("2026-11-21", 50, 200, 180),
            line("Número de operación", 50, 300, 250),
            line("2026-11-22", 400, 300, 180),
            line("123456789012345678", 50, 500, 300)
        ).operationNumber)
    }

    @Test
    fun separatesThePaidAmountFromCommissionAndBalanceInOtherColumns() {
        val result = parse(
            line("S/ 3.50", 550, 240, 150),
            line("S/ 100.00", 550, 100, 150),
            line("S/ 500.00", 550, 380, 150),
            line("Comisión", 50, 240, 200),
            line("Saldo disponible", 50, 380, 250),
            line("Monto transferido", 50, 100, 250)
        )
        assertEquals(VoucherAmount(BigDecimal("100.00"), "PEN"), result.amount)
    }

    @Test
    fun joinsCurrencyAndAmountRecognizedAtDifferentFontSizes() {
        val result = parse(
            line("S/", 50, 112, 50, 30),
            line("25", 115, 100, 120, 60)
        )
        assertEquals(VoucherAmount(BigDecimal("25.00"), "PEN"), result.amount)
    }

    @Test
    fun keepsTheCurrencyForDollarTransfers() {
        val result = parse(
            line("Monto", 50, 100, 150),
            line("US$ 1.250,50", 500, 100, 240),
            line("Comisión", 50, 240, 200),
            line("US$ 3,00", 500, 240, 150)
        )
        assertEquals(VoucherAmount(BigDecimal("1250.50"), "USD"), result.amount)
    }

    @Test
    fun doesNotInventCurrencyOrADecimalSeparator() {
        assertNull(parse(line("Monto: 150.00", 50, 100, 400)).amount)
        assertNull(parse(line("USS 386 74", 50, 100, 400)).amount)
        assertNull(parse(line("USD 386 74", 50, 100, 400)).amount)
    }

    @Test
    fun recoversADollarSymbolConfusionOnlyWithExplicitCurrencyAndAmountLabels() {
        val result = parse(
            line("Moneda:", 50, 100, 150),
            line("Dólares", 500, 100, 150),
            line("Monto:", 50, 300, 150),
            line("USS 386.74", 500, 300, 240)
        )
        assertEquals(VoucherAmount(BigDecimal("386.74"), "USD"), result.amount)
    }

    @Test
    fun doesNotRepairADollarSymbolWithoutCurrencyOrWithAConflictingCurrency() {
        val amount = line("USS 386.74", 500, 300, 240)
        val label = line("Monto:", 50, 300, 150)
        assertNull(parse(label, amount).amount)
        assertNull(parse(
            label, amount, line("Moneda:", 50, 100, 150), line("Soles", 500, 100, 150)
        ).amount)
        assertNull(parse(
            label, amount,
            line("Moneda:", 50, 100, 150), line("Dólares", 500, 100, 150),
            line("Moneda:", 50, 200, 150), line("Soles", 500, 200, 150)
        ).amount)
    }

    @Test
    fun neverInventsMissingDecimalSeparatorsEvenWhenTheCurrencyIsKnown() {
        assertNull(parse(
            line("Moneda:", 50, 100, 150), line("Dólares", 500, 100, 150),
            line("Monto:", 50, 300, 150), line("USS 386 74", 500, 300, 240)
        ).amount)
    }

    @Test
    fun doesNotAttachAnOperationNumberToTheCurrencyOfTheAccount() {
        val result = parse(
            line("Moneda:", 50, 100, 150), line("USD", 500, 100, 100),
            line("Número de operación", 50, 140, 250), line("12345678", 500, 140, 180)
        )
        assertNull(result.amount)
        assertEquals("12345678", result.operationNumber)
    }

    @Test
    fun leavesMultipleTotalsForReviewAndIgnoresCurrencyInADistantLogo() {
        assertNull(parse(
            line("Monto", 50, 100, 150),
            line("S/ 100.00", 500, 100, 200),
            line("Total", 50, 240, 150),
            line("S/ 150.00", 500, 240, 200)
        ).amount)
        assertNull(parse(
            line("S/", 50, 100, 50),
            line("999", 650, 100, 100)
        ).amount)
    }

    @Test
    fun handlesTextWithoutBoundsUsingTheExistingParser() {
        val text = "S/ 150.50\nNúmero de operación: 731546"
        val result = VoucherTextParser.parse(text, listOf(VoucherTextLine(text, 0, 0, 0, 0)))
        assertEquals(VoucherAmount(BigDecimal("150.50"), "PEN"), result.amount)
        assertEquals("731546", result.operationNumber)
    }

    private fun parse(vararg lines: VoucherTextLine) = VoucherTextParser.parse(
        lines.joinToString("\n") { it.text }, lines.toList()
    )

    private fun line(text: String, left: Int, top: Int, width: Int, height: Int = 30) =
        VoucherTextLine(text, left, top, left + width, top + height)
}
