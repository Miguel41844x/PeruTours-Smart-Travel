package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.google.firebase.firestore.DocumentSnapshot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApprovedSalesMapperTest {
    @Test
    fun ignoresPendingRejectedAndNonPositivePayments() {
        assertNull(payment(status = Payment.STATUS_PENDING).toApprovedSaleOrNull())
        assertNull(payment(status = Payment.STATUS_REJECTED).toApprovedSaleOrNull())
        listOf(0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertNull(payment(amount = it).toApprovedSaleOrNull())
        }
    }

    @Test
    fun normalizesLegacyCurrencyCodesForBothDashboards() {
        assertEquals("PEN", payment(currency = " pen ").toApprovedSaleOrNull()?.currency)
        assertEquals("USD", payment(currency = " usd ").toApprovedSaleOrNull()?.currency)
    }

    @Test
    fun prefersTheApprovalDateAndFallsBackWhenItWasNotRecorded() {
        assertEquals(200L, payment(reviewedAt = 200L).toApprovedSaleOrNull()?.approvedAtMillis)
        assertEquals(100L, payment(reviewedAt = 0L).toApprovedSaleOrNull()?.approvedAtMillis)
        assertEquals(100L, payment(reviewedAt = null).toApprovedSaleOrNull()?.approvedAtMillis)
    }

    @Test
    fun usesCostsOnlyFromAMatchingQuotationWithTheSameCurrencyAndTotal() {
        val quote = mockk<DocumentSnapshot> {
            every { getString("currency") } returns "PEN"
            every { getDouble("totalAmount") } returns 120.0
            every { getDouble("baseCost") } returns 100.0
        }
        assertEquals(100.0, payment().toApprovedSaleOrNull(quote)?.estimatedCost)
        every { quote.getString("currency") } returns "USD"
        assertNull(payment().toApprovedSaleOrNull(quote)?.estimatedCost)
        every { quote.getString("currency") } returns "PEN"
        every { quote.getDouble("totalAmount") } returns 500.0
        assertNull(payment().toApprovedSaleOrNull(quote)?.estimatedCost)
        assertNull(payment().toApprovedSaleOrNull()?.estimatedCost)
    }

    private fun payment(
        status: String = Payment.STATUS_APPROVED,
        amount: Double = 120.0,
        currency: String = "PEN",
        reviewedAt: Long? = 200L
    ): DocumentSnapshot = mockk {
        every { getString("status") } returns status
        every { getDouble("amount") } returns amount
        every { getString("currency") } returns currency
        every { getLong("reviewedAtMillis") } returns reviewedAt
        every { getLong("submittedAtMillis") } returns 100L
    }
}
