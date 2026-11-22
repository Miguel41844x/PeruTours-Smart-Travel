package com.example.perutours.domain.payment

import java.math.BigDecimal
import java.math.RoundingMode

data class VoucherOcrResult(
    val amount: VoucherAmount?,
    val operationNumber: String?,
    val transactionDate: VoucherDate? = null,
    val transactionTime: VoucherTime? = null
) {
    val hasData: Boolean
        get() = amount != null || operationNumber != null ||
            transactionDate != null || transactionTime != null
}

/** Valores locales impresos en el comprobante, sin conversión de zona horaria. */
data class VoucherDate(val year: Int, val month: Int, val day: Int)

data class VoucherTime(val hour: Int, val minute: Int, val second: Int? = null)

data class VoucherAmount(val value: BigDecimal, val currency: String) {
    fun matches(expectedAmount: Double, expectedCurrency: String): Boolean =
        currency.equals(expectedCurrency.trim(), ignoreCase = true) &&
            value.compareTo(BigDecimal.valueOf(expectedAmount).setScale(2, RoundingMode.HALF_UP)) == 0
}
