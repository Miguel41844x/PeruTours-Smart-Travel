package com.example.perutours.ui.screens.admin.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
import java.util.Locale

@Composable
fun MonthlyRevenueVicoChart(
    monthlyRevenue: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    if (monthlyRevenue.isEmpty() || monthlyRevenue.values.all { it <= 0.0 }) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aún no hay ingresos registrados",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val labels = remember(monthlyRevenue) { monthlyRevenue.keys.toList() }
    val values = remember(monthlyRevenue) { monthlyRevenue.values.map { it.toFloat() } }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values) {
        modelProducer.runTransaction { columnSeries { series(values) } }
    }

    // Eje X: nombre del mes. Eje Y: monto en soles abreviado ("S/ 1.2k").
    val monthFormatter = remember(labels) {
        CartesianValueFormatter { _, x, _ -> labels.getOrElse(x.toInt()) { "" } }
    }
    val amountFormatter = remember {
        CartesianValueFormatter { _, y, _ ->
            if (y >= 1000) String.format(Locale.US, "S/ %.1fk", y / 1000.0) else "S/ ${y.toInt()}"
        }
    }

    // Aplica los colores de Material 3 (ladrillo, maíz, valle) y el modo oscuro.
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(valueFormatter = amountFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = monthFormatter)
            ),
            modelProducer = modelProducer,
            modifier = modifier.fillMaxWidth().height(220.dp)
        )
    }
}
