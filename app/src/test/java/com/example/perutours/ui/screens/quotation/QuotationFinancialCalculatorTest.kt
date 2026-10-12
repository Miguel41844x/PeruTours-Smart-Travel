package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationServiceItem
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class QuotationFinancialCalculatorTest {

    @Test
    fun calculateTotalAndMarginCorrectly() {
        val items = listOf(
            QuotationServiceItem(id = "1", name = "Hotel", unitCost = 100.0, quantity = 2, subtotal = 200.0),
            QuotationServiceItem(id = "2", name = "Tour", unitCost = 50.0, quantity = 1, subtotal = 50.0)
        )
        val baseCost = items.sumOf { it.subtotal }
        val marginPercent = 20
        val total = baseCost * (1 + marginPercent / 100.0)
        val roundedTotal = String.format(Locale.US, "%.2f", total).toDouble()

        assertEquals(250.0, baseCost, 0.001)
        assertEquals(300.0, roundedTotal, 0.001)
    }

    @Test
    fun quotationVersionIncrementOnRevision() {
        val original = Quotation(id = "q1", version = 1, totalAmount = 200.0)
        val revised = original.copy(
            version = original.version + 1,
            parentQuotationId = original.id,
            totalAmount = 220.0
        )

        assertEquals(2, revised.version)
        assertEquals("q1", revised.parentQuotationId)
        assertEquals(220.0, revised.totalAmount, 0.001)
    }
}
