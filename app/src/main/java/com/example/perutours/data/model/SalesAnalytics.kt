package com.example.perutours.data.model

data class SalesAnalytics(
    val totalQuotations: Int = 0,
    val approvedSales: Int = 0,
    val conversionRate: Double = 0.0,
    val quotationStatuses: List<QuotationStatusMetric> = emptyList(),
    val revenueByCurrency: List<CurrencyRevenue> = emptyList(),
    val topDestinations: List<DestinationDemandMetric> = emptyList(),
    val updatedAtMillis: Long = 0L
) {
    val hasData: Boolean
        get() = totalQuotations > 0 || approvedSales > 0 || topDestinations.isNotEmpty()
}

data class CurrencyRevenue(
    val currency: String = "PEN",
    val totalAmount: Double = 0.0,
    val monthlyRevenue: List<MonthlyRevenueMetric> = emptyList()
)

data class QuotationStatusMetric(
    val status: String,
    val count: Int
)

data class MonthlyRevenueMetric(
    val periodKey: String,
    val label: String,
    val amount: Double
)

data class DestinationDemandMetric(
    val destination: String,
    val requestCount: Int,
    val percentage: Double
)
