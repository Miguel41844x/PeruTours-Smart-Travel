package com.example.perutours.ui.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.animation.simpleChartAnimation
import kotlin.math.roundToInt

@Composable
fun ReservationStatusDonutChart(
    statusData: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    if (statusData.isEmpty() || statusData.values.all { it <= 0 }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aún no hay reservas registradas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = statusData.values.sum().coerceAtLeast(1)

    val slices = statusData.map { (status, count) ->
        PieChartData.Slice(
            value = count.toFloat(),
            color = getStatusColor(status)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                pieChartData = PieChartData(slices = slices),
                modifier = Modifier.fillMaxSize(),
                animation = simpleChartAnimation()
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            statusData.forEach { (status, count) ->
                val percentage = (count * 100f / total).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = getStatusColor(status)
                        ) {
                            Box(modifier = Modifier.size(10.dp))
                        }
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "$count ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        "Pagado" -> MaterialTheme.colorScheme.tertiary
        "Pago enviado" -> MaterialTheme.colorScheme.secondary
        "Confirmada" -> MaterialTheme.colorScheme.primary
        "Pago rechazado" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outline
    }
}
