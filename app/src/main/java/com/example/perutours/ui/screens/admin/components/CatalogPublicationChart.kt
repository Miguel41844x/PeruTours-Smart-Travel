package com.example.perutours.ui.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perutours.data.model.CatalogStatus
import com.github.tehras.charts.piechart.PieChart
import com.github.tehras.charts.piechart.PieChartData
import com.github.tehras.charts.piechart.animation.simpleChartAnimation

@Composable
fun CatalogPublicationChart(counts: Map<String, Int>) {
    val colors = MaterialTheme.colorScheme
    val entries = listOf(
        Triple(CatalogStatus.PUBLICADO, "Publicados", colors.primary),
        Triple(CatalogStatus.BORRADOR, "Borradores", colors.secondary),
        Triple(CatalogStatus.ARCHIVADO, "Archivados", colors.outline)
    ).filter { (counts[it.first.name] ?: 0) > 0 }
    if (entries.isEmpty()) {
        Text("Registra tu primer paquete para ver su estado de publicación.", color = colors.onSurfaceVariant)
        return
    }
    PieChart(PieChartData(entries.map { PieChartData.Slice((counts[it.first.name] ?: 0).toFloat(), it.third) }),
        modifier = Modifier.fillMaxWidth().height(180.dp), animation = simpleChartAnimation())
    entries.forEach { (status, label, color) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = color, shape = MaterialTheme.shapes.small) { Spacer(Modifier.size(12.dp)) }
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text((counts[status.name] ?: 0).toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}
