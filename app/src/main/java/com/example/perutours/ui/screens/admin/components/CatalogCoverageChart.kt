package com.example.perutours.ui.screens.admin.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries

@Composable
fun CatalogCoverageChart(services: Map<String, Int>) {
    if (services.isEmpty()) {
        Text("Aún no hay servicios disponibles en destinos publicados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val labels = services.keys.toList()
    val values = services.values.toList()
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) { modelProducer.runTransaction { columnSeries { series(values) } } }
    val labelsFormatter = remember(labels) {
        CartesianValueFormatter { _, value, _ -> labels.getOrElse(value.toInt()) { "" }.take(12) }
    }
    val countFormatter = remember { CartesianValueFormatter { _, value, _ -> value.toInt().toString() } }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(valueFormatter = countFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = labelsFormatter)),
            modelProducer = modelProducer, modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}
