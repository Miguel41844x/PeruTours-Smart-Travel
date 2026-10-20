package com.example.perutours.data.model

data class SalesAnalytics(
    val totalQuotations: Int = 0,
    val approvedSales: Int = 0,
    val totalRevenue: Double = 0.0,
    val currency: String = "PEN",
    val conversionRate: Double = 0.0,
    val quotationStatuses: List<QuotationStatusMetric> = emptyList(),
    val monthlyRevenue: List<MonthlyRevenueMetric> = emptyList(),
    val topDestinations: List<DestinationDemandMetric> = emptyList(),
    val updatedAtMillis: Long = 0L
) {
    val hasData: Boolean
        get() = totalQuotations > 0 || approvedSales > 0 || topDestinations.isNotEmpty()
}

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
