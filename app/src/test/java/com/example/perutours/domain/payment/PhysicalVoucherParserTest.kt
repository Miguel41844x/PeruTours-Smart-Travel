package com.example.perutours.domain.payment

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhysicalVoucherParserTest {
    @Test
    fun readsPrintedDepositWithMaskedAmountAndOperationBeforeTheDate() {
        val result = VoucherTextParser.parse(
            """
                DEPOSITO CUENTA AHORROS
                OF. /191063-063C-S49093 OP-0929737 04/04/2017
                Hora:13:04:32
                CODIGO DE CUENTA: 193-00000000-0-00
                IMPORTE DEPOSITADO: S/. ****** 620.00
                Importe en monedas: S/. ********** 20.00
                Comision: S/. ***** 3.00
            """.trimIndent()
        )

        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"), result.amount)
        assertEquals("0929737", result.operationNumber)
        assertEquals(VoucherDate(2017, 4, 4), result.transactionDate)
        assertEquals(VoucherTime(13, 4, 32), result.transactionTime)
    }

    @Test
    fun readsAbbreviatedOperationLabelsWithoutChangingTheIdentifier() {
        listOf("OP-", "OP:", "OP.", "0P-").forEach { label ->
            assertEquals(label, "00123456",
                VoucherTextParser.parse("$label 00123456").operationNumber)
        }
        assertEquals("00AB1234", VoucherTextParser.parse("OP: 00AB1234").operationNumber)
        assertEquals("00123456", VoucherTextParser.parse("OP-\n00123456").operationNumber)
    }

    @Test
    fun leavesUnlabeledConflictingAndIncompleteOperationCodesForReview() {
        listOf(
            "OF-191063-063C-S49093",
            "COP-00123456",
            "CCI: 00219300000000000000",
            "OP-2026-11-21",
            "OP-04/04/2017",
            "OP-0012 3456",
            "OP-00123456 00987654",
            "OP-00123456 OP-00987654",
            "OP-00123456\nOP-00987654"
        ).forEach { text ->
            assertNull(text, VoucherTextParser.parse(text).operationNumber)
        }
    }

    @Test
    fun acceptsOnlyPaddingBeforeACompleteAmountWithExplicitCurrency() {
        listOf("S/. ******620.00", "S/, *** 620,00", "PEN * * *620.00").forEach { text ->
            assertEquals(text, VoucherAmount(BigDecimal("620.00"), "PEN"),
                VoucherTextParser.parse(text).amount)
        }
        assertEquals(VoucherAmount(BigDecimal("1250.50"), "USD"),
            VoucherTextParser.parse("USD ******1,250.50").amount)
        listOf(
            "S/. 12****620.00",
            "S/. ******620.00ABC",
            "S/. ******620.0",
            "S/. K***X* 820.UU",
            "S/. *****x $20.00",
            "IMPORTE DEPOSITADO: ******620.00"
        ).forEach { text ->
            assertNull(text, VoucherTextParser.parse(text).amount)
        }
    }

    @Test
    fun toleratesOcrSpacesAfterAnExistingDecimalSeparator() {
        listOf("S/ . ****620. 00", "S/, ****620, 00", "PEN ****620.\t00").forEach { text ->
            assertEquals(text, VoucherAmount(BigDecimal("620.00"), "PEN"),
                VoucherTextParser.parse(text).amount)
        }
        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"), parse(
            line("IMPORTE DEPOSITADO:", 50, 300, 300),
            line("S/. ****", 450, 300, 180),
            line("620. 00", 650, 300, 150)
        ).amount)
        listOf("S/.620 00", "S/.620. 0", "S/.620. 000", "S/.620.\n00").forEach { text ->
            assertNull(text, VoucherTextParser.parse(text).amount)
        }
    }

    @Test
    fun doesNotReplaceAnUnreadableTotalWithCoinsBillsOrCommission() {
        listOf("Importe en monedas", "Importe en billetes", "Comision", "Saldo").forEach { label ->
            assertNull(VoucherTextParser.parse(
                "IMPORTE DEPOSITADO: S/. *****820.UU\n$label\nS/. *****20.00"
            ).amount)
            assertNull(parse(
                line("IMPORTE DEPOSITADO:", 50, 100, 300),
                line("S/. *****820.UU", 600, 100, 300),
                line(label, 50, 300, 300),
                line("S/. *****20.00", 600, 300, 300)
            ).amount)
        }
    }

    @Test
    fun doesNotConfuseAnUnlabeledPaymentAfterTheDateWithAPreviousField() {
        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"),
            VoucherTextParser.parse("Fecha: 21/11/2026\nS/ 620.00").amount)
        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"),
            VoucherTextParser.parse("Comision: S/ 3.00\nS/ 620.00").amount)
        assertNull(VoucherTextParser.parse("CODIGO DE CUENTA: S/ 12345678").amount)
        assertNull(VoucherTextParser.parse("CCI: S/ 12345678").amount)
    }

    @Test
    fun extractsTheOperationButLeavesTheUnclearPhotographedAmountForReview() {
        val result = parse(
            line("OF./191063-063C-S49093 0P-0929737 04/04/2017", 50, 100, 900),
            line("Hora:13:04:32", 650, 180, 300),
            line("IMPORTE DEPOSITADO:", 50, 400, 300),
            line("S/, K***X* 820.UU", 650, 400, 300),
            line("Importe en monedas:", 50, 600, 300),
            line("S/.**********0.00", 650, 600, 300)
        )
        assertEquals("0929737", result.operationNumber)
        assertNull(result.amount)
        assertEquals(VoucherDate(2017, 4, 4), result.transactionDate)
        assertEquals(VoucherTime(13, 4, 32), result.transactionTime)
    }

    @Test
    fun readsAReceiptWhoseColumnsWereRecognizedOutOfOrder() {
        val result = parse(
            line("S/. ******620.00", 600, 400, 300),
            line("S/. ******20.00", 600, 600, 300),
            line("Importe en monedas:", 50, 600, 300),
            line("IMPORTE DEPOSITADO:", 50, 400, 300),
            line("OF. /191063 0P-0929737 04/04/2017", 50, 100, 850),
            line("Hora:13:04:32", 600, 200, 300)
        )

        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"), result.amount)
        assertEquals("0929737", result.operationNumber)
        assertEquals(VoucherDate(2017, 4, 4), result.transactionDate)
        assertEquals(VoucherTime(13, 4, 32), result.transactionTime)
    }

    @Test
    fun joinsCurrencyPaddingAndAmountWithoutUsingTheAccountNumber() {
        val result = parse(
            line("IMPORTE DEPOSITADO:", 50, 300, 300),
            line("S/. ******", 450, 300, 180),
            line("620.00", 650, 300, 150),
            line("CODIGO DE CUENTA:", 50, 100, 300),
            line("19300000000000", 600, 100, 300)
        )

        assertEquals(VoucherAmount(BigDecimal("620.00"), "PEN"), result.amount)
        assertNull(result.operationNumber)
    }

    @Test
    fun doesNotChooseBetweenTwoDifferentPrintedTotals() {
        val text = "IMPORTE DEPOSITADO: S/. ****620.00\nTOTAL: S/. ****720.00"
        assertNull(VoucherTextParser.parse(text).amount)
        assertNull(parse(
            line("IMPORTE DEPOSITADO: S/. ****620.00", 50, 100, 850),
            line("TOTAL: S/. ****720.00", 50, 300, 850)
        ).amount)
    }

    private fun parse(vararg lines: VoucherTextLine) =
        VoucherTextParser.parse(lines.joinToString("\n") { it.text }, lines.toList())

    private fun line(text: String, left: Int, top: Int, width: Int) =
        VoucherTextLine(text, left, top, left + width, top + 40)
}
