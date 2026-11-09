package com.example.perutours.ui.screens.sales.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perutours.data.model.DestinationDemandMetric
import com.github.tehras.charts.bar.BarChart
import com.github.tehras.charts.bar.BarChartData
import com.github.tehras.charts.bar.renderer.bar.SimpleBarDrawer
import com.github.tehras.charts.bar.renderer.label.SimpleValueDrawer
import com.github.tehras.charts.bar.renderer.xaxis.SimpleXAxisDrawer
import com.github.tehras.charts.bar.renderer.yaxis.SimpleYAxisDrawer
import com.github.tehras.charts.piechart.animation.simpleChartAnimation

/**
 * HU09: destinos más solicitados con Tehras Charts (gráfico nativo de Compose).
 * Muestra el top 5; el detalle con porcentajes sigue en [DestinationRanking].
 */
@Composable
fun DestinationDemandBarChart(
    destinations: List<DestinationDemandMetric>,
    modifier: Modifier = Modifier
) {
    if (destinations.isEmpty()) return

    val colors = MaterialTheme.colorScheme
    val palette = listOf(
        colors.primary, colors.tertiary, colors.secondary, colors.error, colors.onSurfaceVariant
    )

    val bars = destinations.take(5).mapIndexed { index, destination ->
        BarChartData.Bar(
            label = destination.destination.shortLabel(),
            value = destination.requestCount.toFloat(),
            color = palette[index % palette.size]
        )
    }

    BarChart(
        barChartData = BarChartData(bars = bars),
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        animation = simpleChartAnimation(),
        barDrawer = SimpleBarDrawer(),
        xAxisDrawer = SimpleXAxisDrawer(axisLineColor = colors.outline),
        yAxisDrawer = SimpleYAxisDrawer(
            axisLineColor = colors.outline,
            labelTextColor = colors.onSurfaceVariant
        ),
        labelDrawer = SimpleValueDrawer(
            SimpleValueDrawer.DrawLocation.Outside,
            labelTextColor = colors.onSurface
        )
    )
}

private fun String.shortLabel(): String {
    val city = substringBefore(',')
    return if (city.length > 10) city.take(9) + "…" else city
}
