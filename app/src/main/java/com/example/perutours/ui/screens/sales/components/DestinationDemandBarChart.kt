package com.example.perutours.ui.screens.sales.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    val palette = listOf(
        androidx.compose.ui.graphics.Color(0xFF8F2D24),
        androidx.compose.ui.graphics.Color(0xFFC48712),
        androidx.compose.ui.graphics.Color(0xFF176B5B),
        androidx.compose.ui.graphics.Color(0xFF777872),
        androidx.compose.ui.graphics.Color(0xFFBA1A1A)
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
        xAxisDrawer = SimpleXAxisDrawer(),
        yAxisDrawer = SimpleYAxisDrawer(),
        labelDrawer = SimpleValueDrawer(SimpleValueDrawer.DrawLocation.Outside)
    )
}

private fun String.shortLabel(): String = if (length > 10) take(9) + "…" else this
