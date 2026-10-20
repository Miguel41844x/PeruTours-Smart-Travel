package com.example.perutours.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class SalesAnalyticsCalculatorTest {
    private val timeZone = TimeZone.getTimeZone("America/Lima")
    private val calculator = SalesAnalyticsCalculator(timeZone)

    @Test
    fun calculatesQuotationStatusRevenueAndConversion() {
        val analytics = calculator.calculate(
            quotationStatuses = listOf("Cotizado", "Aceptada", "Observada", "Cancelada"),
            approvedSales = listOf(
                ApprovedSaleRecord(800.0, "PEN", date(2026, Calendar.OCTOBER, 2)),
                ApprovedSaleRecord(200.0, "PEN", date(2026, Calendar.OCTOBER, 4)),
                ApprovedSaleRecord(99.0, "USD", date(2026, Calendar.OCTOBER, 5))
            ),
            requestedDestinations = emptyList(),
            nowMillis = date(2026, Calendar.OCTOBER, 20)
        )

        assertEquals(4, analytics.totalQuotations)
        assertEquals(2, analytics.approvedSales)
        assertEquals(1_000.0, analytics.totalRevenue, 0.0)
        assertEquals(50.0, analytics.conversionRate, 0.0)
        assertEquals(listOf("Cotizado", "Aceptada", "Observada", "Cancelada"), analytics.quotationStatuses.map { it.status })
    }

    @Test
    fun fillsTheLastSixMonthsAndGroupsApprovedRevenue() {
        val analytics = calculator.calculate(
            quotationStatuses = emptyList(),
            approvedSales = listOf(
                ApprovedSaleRecord(300.0, "PEN", date(2025, Calendar.DECEMBER, 10)),
                ApprovedSaleRecord(700.0, "PEN", date(2026, Calendar.JANUARY, 8))
            ),
            requestedDestinations = emptyList(),
            nowMillis = date(2026, Calendar.MARCH, 15)
        )

        assertEquals(
            listOf("2025-10", "2025-11", "2025-12", "2026-01", "2026-02", "2026-03"),
            analytics.monthlyRevenue.map { it.periodKey }
        )
        assertEquals(listOf(0.0, 0.0, 300.0, 700.0, 0.0, 0.0), analytics.monthlyRevenue.map { it.amount })
    }

    @Test
    fun ranksDestinationsIgnoringCaseAndExtraSpaces() {
        val analytics = calculator.calculate(
            quotationStatuses = emptyList(),
            approvedSales = emptyList(),
            requestedDestinations = listOf(
                "Cusco, Perú",
                "cusco, perú",
                "  Arequipa,   Perú ",
                "Cusco, Perú"
            ),
            nowMillis = date(2026, Calendar.OCTOBER, 20)
        )

        assertEquals("Cusco, Perú", analytics.topDestinations.first().destination)
        assertEquals(3, analytics.topDestinations.first().requestCount)
        assertEquals(75.0, analytics.topDestinations.first().percentage, 0.0)
        assertEquals("Arequipa, Perú", analytics.topDestinations[1].destination)
    }

    private fun date(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
