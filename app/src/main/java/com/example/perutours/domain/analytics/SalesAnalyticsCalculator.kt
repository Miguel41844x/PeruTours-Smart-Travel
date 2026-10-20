package com.example.perutours.domain.analytics

import com.example.perutours.data.model.DestinationDemandMetric
import com.example.perutours.data.model.MonthlyRevenueMetric
import com.example.perutours.data.model.QuotationStatusMetric
import com.example.perutours.data.model.SalesAnalytics
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ApprovedSaleRecord(
    val amount: Double,
    val currency: String,
    val approvedAtMillis: Long
)

class SalesAnalyticsCalculator(
    private val timeZone: TimeZone = TimeZone.getTimeZone("America/Lima")
) {
    fun calculate(
        quotationStatuses: List<String>,
        approvedSales: List<ApprovedSaleRecord>,
        requestedDestinations: List<String>,
        nowMillis: Long = System.currentTimeMillis(),
        currency: String = DEFAULT_CURRENCY
    ): SalesAnalytics {
        val salesInCurrency = approvedSales.filter {
            it.currency.equals(currency, ignoreCase = true)
        }
        val quotationMetrics = quotationStatuses
            .groupingBy(::canonicalStatus)
            .eachCount()
            .map { (status, count) -> QuotationStatusMetric(status, count) }
            .sortedWith(compareBy({ STATUS_ORDER.indexOf(it.status) }, { it.status }))

        return SalesAnalytics(
            totalQuotations = quotationStatuses.size,
            approvedSales = salesInCurrency.size,
            totalRevenue = salesInCurrency.sumOf { it.amount },
            currency = currency.uppercase(Locale.ROOT),
            conversionRate = if (quotationStatuses.isEmpty()) {
                0.0
            } else {
                salesInCurrency.size * 100.0 / quotationStatuses.size
            },
            quotationStatuses = quotationMetrics,
            monthlyRevenue = monthlyRevenue(salesInCurrency, nowMillis),
            topDestinations = destinationRanking(requestedDestinations),
            updatedAtMillis = nowMillis
        )
    }

    private fun monthlyRevenue(
        sales: List<ApprovedSaleRecord>,
        nowMillis: Long
    ): List<MonthlyRevenueMetric> {
        val periods = recentPeriods(nowMillis)
        val totals = sales
            .filter { it.approvedAtMillis > 0L }
            .groupBy { periodKey(it.approvedAtMillis) }
            .mapValues { (_, records) -> records.sumOf { it.amount } }

        return periods.map { period ->
            MonthlyRevenueMetric(
                periodKey = period.key,
                label = period.label,
                amount = totals[period.key] ?: 0.0
            )
        }
    }

    private fun destinationRanking(destinations: List<String>): List<DestinationDemandMetric> {
        val normalized = destinations
            .map(::cleanLabel)
            .filter { it.isNotBlank() }
        if (normalized.isEmpty()) return emptyList()

        return normalized
            .groupBy { it.lowercase(Locale.ROOT) }
            .map { (_, labels) ->
                DestinationDemandMetric(
                    destination = labels.first(),
                    requestCount = labels.size,
                    percentage = labels.size * 100.0 / normalized.size
                )
            }
            .sortedWith(
                compareByDescending<DestinationDemandMetric> { it.requestCount }
                    .thenBy { it.destination }
            )
            .take(MAX_DESTINATIONS)
    }

    private fun recentPeriods(nowMillis: Long): List<MonthPeriod> {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -(MONTH_WINDOW - 1))
        }

        return List(MONTH_WINDOW) {
            val month = calendar.get(Calendar.MONTH)
            val period = MonthPeriod(
                key = "%04d-%02d".format(
                    Locale.ROOT,
                    calendar.get(Calendar.YEAR),
                    month + 1
                ),
                label = MONTH_LABELS[month]
            )
            calendar.add(Calendar.MONTH, 1)
            period
        }
    }

    private fun periodKey(timestamp: Long): String {
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = timestamp }
        return "%04d-%02d".format(
            Locale.ROOT,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    private fun canonicalStatus(value: String): String = when (normalize(value)) {
        "cotizado" -> "Cotizado"
        "aceptada" -> "Aceptada"
        "observada" -> "Observada"
        "cancelada" -> "Cancelada"
        else -> "Otros"
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value.trim(), Normalizer.Form.NFD)
        .replace(ACCENTS_REGEX, "")
        .lowercase(Locale.ROOT)

    private fun cleanLabel(value: String): String = value
        .trim()
        .replace(WHITESPACE_REGEX, " ")

    private data class MonthPeriod(val key: String, val label: String)

    private companion object {
        const val DEFAULT_CURRENCY = "PEN"
        const val MONTH_WINDOW = 6
        const val MAX_DESTINATIONS = 5

        val ACCENTS_REGEX = "\\p{M}+".toRegex()
        val WHITESPACE_REGEX = "\\s+".toRegex()
        val STATUS_ORDER = listOf("Cotizado", "Aceptada", "Observada", "Cancelada", "Otros")
        val MONTH_LABELS = listOf(
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
        )
    }
}
