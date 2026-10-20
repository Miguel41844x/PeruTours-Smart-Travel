package com.example.perutours.ui.screens.sales.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.perutours.data.model.MonthlyRevenueMetric
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.util.Locale

@Composable
fun MonthlyRevenueLineChart(
    metrics: List<MonthlyRevenueMetric>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aún no hay ingresos para este periodo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                setNoDataText("Aún no hay ingresos para este periodo.")
                extraBottomOffset = 8f
            }
        },
        update = { chart ->
            val entries = metrics.mapIndexed { index, metric ->
                Entry(index.toFloat(), metric.amount.toFloat())
            }
            val dataSet = LineDataSet(entries, "Ingresos").apply {
                color = primaryColor
                circleColors = listOf(primaryColor)
                lineWidth = 3f
                circleRadius = 4f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = primaryColor
                fillAlpha = 28
            }

            chart.axisLeft.textColor = axisColor
            chart.axisLeft.gridColor = gridColor
            chart.axisLeft.valueFormatter = CurrencyAxisFormatter(currency)
            chart.xAxis.textColor = axisColor
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(metrics.map { it.label })
            chart.data = LineData(dataSet)
            chart.animateX(500)
            chart.invalidate()
        }
    )
}

private class CurrencyAxisFormatter(currency: String) : ValueFormatter() {
    private val prefix = if (currency.equals("PEN", ignoreCase = true)) "S/ " else "$currency "
    private val numberFormat = NumberFormat.getIntegerInstance(
        Locale.Builder().setLanguage("es").setRegion("PE").build()
    )

    override fun getFormattedValue(value: Float): String = prefix + numberFormat.format(value)
}

private val CHART_HEIGHT = 240.dp
