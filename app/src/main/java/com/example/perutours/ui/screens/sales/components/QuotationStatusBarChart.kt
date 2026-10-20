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
import com.example.perutours.data.model.QuotationStatusMetric
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

@Composable
fun QuotationStatusBarChart(
    metrics: List<QuotationStatusMetric>,
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
                text = "Aún no hay cotizaciones para comparar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val barColors = listOf(
        MaterialTheme.colorScheme.primary.toArgb(),
        MaterialTheme.colorScheme.tertiary.toArgb(),
        MaterialTheme.colorScheme.secondary.toArgb(),
        MaterialTheme.colorScheme.error.toArgb()
    )

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(CHART_HEIGHT),
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f
                axisLeft.granularity = 1f
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.granularity = 1f
                xAxis.setDrawGridLines(false)
                setScaleEnabled(false)
                setPinchZoom(false)
                setDrawValueAboveBar(true)
                setNoDataText("Aún no hay cotizaciones para comparar.")
                extraBottomOffset = 8f
            }
        },
        update = { chart ->
            val entries = metrics.mapIndexed { index, metric ->
                BarEntry(index.toFloat(), metric.count.toFloat())
            }
            val dataSet = BarDataSet(entries, "Cotizaciones").apply {
                colors = barColors
                valueTextColor = axisColor
                valueTextSize = 11f
                valueFormatter = WholeNumberFormatter
            }

            chart.axisLeft.textColor = axisColor
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.textColor = axisColor
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(metrics.map { it.status })
            chart.data = BarData(dataSet).apply { barWidth = 0.58f }
            chart.setFitBars(true)
            chart.animateY(450)
            chart.invalidate()
        }
    )
}

private object WholeNumberFormatter : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = value.toInt().toString()
}

private val CHART_HEIGHT = 240.dp
