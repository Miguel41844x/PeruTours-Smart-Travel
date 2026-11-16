package com.example.perutours.domain.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class VoucherTextParserTest {
    @Test
    fun readsSolesAndKeepsLeadingZerosInTheOperationNumber() {
        val result = VoucherTextParser.parse("Yape\nS/ 1,250.50\nN.º de operación:\n0012345")

        assertEquals(VoucherAmount(BigDecimal("1250.50"), "PEN"), result.amount)
        assertEquals("0012345", result.operationNumber)
        assertTrue(result.amount!!.matches(1250.50, "PEN"))
        assertFalse(result.amount.matches(1250.50, "USD"))
        assertFalse(result.amount.matches(1250.51, "PEN"))
    }

    @Test
    fun prefersLabeledPaymentOverCommissionAndSupportsLocalSeparators() {
        val result = VoucherTextParser.parse(
            "Transferencia BCP\nMonto transferido\nUS$ 1.250,50\nComisión: US$ 3,00"
        )
        assertEquals(VoucherAmount(BigDecimal("1250.50"), "USD"), result.amount)
        assertEquals(result.amount, VoucherTextParser.parse("USD 1 250,50").amount)
    }

    @Test
    fun avoidsGuessingWhenThereAreMultipleAmountsOrAnUnlabeledCode() {
        val result = VoucherTextParser.parse(
            "Plin\nS/ 150.00\nS/ 100.00\nCódigo de seguridad: 8456\n07/11/2026"
        )
        assertNull(result.amount)
        assertNull(result.operationNumber)
        assertNull(VoucherTextParser.parse("S/ 1.234,5").amount)
        assertNull(VoucherTextParser.parse("Total: 150.00").amount)
        assertNull(VoucherTextParser.parse("Monto transferido\nComisión: S/ 3.00").amount)
    }
}
