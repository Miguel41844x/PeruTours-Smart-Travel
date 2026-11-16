package com.example.perutours.domain.payment

import java.math.BigDecimal
import java.math.RoundingMode

data class VoucherOcrResult(
    val amount: VoucherAmount?,
    val operationNumber: String?
)

data class VoucherAmount(val value: BigDecimal, val currency: String) {
    fun matches(expectedAmount: Double, expectedCurrency: String): Boolean =
        currency.equals(expectedCurrency.trim(), ignoreCase = true) &&
            value.compareTo(BigDecimal.valueOf(expectedAmount).setScale(2, RoundingMode.HALF_UP)) == 0
}
