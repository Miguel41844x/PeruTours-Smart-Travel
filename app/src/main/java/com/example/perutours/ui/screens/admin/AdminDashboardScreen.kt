package com.example.perutours.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.Reservation
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onManageDestinations: () -> Unit,
    onManagePackages: () -> Unit,
    onManageProviders: () -> Unit,
    onNavigateBack: () -> Unit = {},
    viewModel: AdminDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Dashboard administrativo")
                        Text(
                            text = "Indicadores y analítica del negocio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar datos")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.metrics.totalReservations == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = viewModel::refresh) { Text("Reintentar") }
                        }
                    }
                }
            }

            if (uiState.metrics.isCached) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                text = "Sin conexión. Mostrando la última información guardada, actualizada el ${uiState.formattedUpdatedAt}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // --- Section: KPI Summary Cards ---
            item {
                Text(
                    text = "Indicadores clave (KPIs)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Wide Revenue Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Ingresos totales (Pagados)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = formatCurrency(uiState.metrics.totalRevenue),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Two Smaller Cards for Reservations and Active Customers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KpiCard(
                            title = "Total reservas",
                            value = uiState.metrics.totalReservations.toString(),
                            icon = Icons.Default.Bookmark,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = "Clientes activos",
                            value = uiState.metrics.activeCustomers.toString(),
                            icon = Icons.Default.Group,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Section: Numerical Summary by Status ---
            item {
                Text(
                    text = "Resumen de resultados por estado",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (uiState.metrics.statusSummary.isEmpty()) {
                            Text("No hay reservas registradas aún.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val total = uiState.metrics.totalReservations.coerceAtLeast(1)
                            uiState.metrics.statusSummary.forEach { (status, count) ->
                                val progress = count.toFloat() / total.toFloat()
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                         Surface(
                                             shape = RoundedCornerShape(4.dp),
                                             color = getStatusColor(status)
                                         ) {
                                             Box(modifier = Modifier.size(10.dp))
                                         }
                                         Text(status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                     }
                                     Text("$count (${(progress * 100).toInt()}%)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                 }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = getStatusColor(status),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Charts ---
            item {
                Text(
                    text = "Comportamiento e ingresos",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Ingresos por mes (Pagados)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        MonthlyRevenueChart(monthlyData = uiState.metrics.monthlyRevenue)
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Distribución de reservas por estado", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        ReservationStatusChart(statusData = uiState.metrics.statusSummary)
                    }
                }
            }

            // --- Section: Paginated Reservations List with Horizontal Filter Chips ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listado de reservas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.reservations.size} visibles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AdminDashboardConstants.AVAILABLE_FILTERS) { filter ->
                        FilterChip(
                            selected = uiState.selectedFilterStatus == filter,
                            onClick = { viewModel.setFilterStatus(filter) },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            if (uiState.reservations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("No se encontraron reservas con este filtro.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = { viewModel.setFilterStatus("Todos") }) {
                                Text("Limpiar filtro")
                            }
                        }
                    }
                }
            } else {
                items(uiState.reservations, key = { it.id }) { reservation ->
                    ReservationItemCard(reservation = reservation)
                }

                if (!uiState.endReached) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            } else {
                                OutlinedButton(onClick = viewModel::loadMoreReservations) {
                                    Text("Cargar más reservas")
                                }
                            }
                        }
                    }
                }
            }

            // --- Section: Catalog Management Shortcuts (Bottom) ---
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Gestión del catálogo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CatalogShortcutCard(
                        title = "Destinos",
                        icon = Icons.Default.Place,
                        modifier = Modifier.weight(1f),
                        onClick = onManageDestinations
                    )
                    CatalogShortcutCard(
                        title = "Paquetes",
                        icon = Icons.Default.Inventory2,
                        modifier = Modifier.weight(1f),
                        onClick = onManagePackages
                    )
                    CatalogShortcutCard(
                        title = "Proveedores",
                        icon = Icons.Default.SupportAgent,
                        modifier = Modifier.weight(1f),
                        onClick = onManageProviders
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogShortcutCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(96.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = contentColor)
        }
    }
}

@Composable
private fun ReservationItemCard(reservation: Reservation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.destination.ifBlank { "Destino general" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Badge(
                    containerColor = getStatusColor(reservation.status)
                ) {
                    Text(reservation.status, color = MaterialTheme.colorScheme.onError)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCurrency(reservation.totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDate(reservation.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthlyRevenueChart(monthlyData: Map<String, Double>) {
    if (monthlyData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay datos de ingresos pagados.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()

    val entries = monthlyData.entries.mapIndexed { index, entry ->
        Entry(index.toFloat(), entry.value.toFloat())
    }
    val labels = monthlyData.keys.toList()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, "Ingresos").apply {
                color = primaryColor
                circleColors = listOf(primaryColor)
                lineWidth = 2.5f
                circleRadius = 3.5f
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawValues(false)
                setDrawFilled(true)
                fillColor = primaryColor
                fillAlpha = 35
            }

            chart.axisLeft.textColor = axisColor
            chart.axisLeft.gridColor = gridColor
            chart.axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value >= 1000f) {
                        return String.format(Locale.US, "S/ %.1fk", value / 1000f)
                    }
                    return "S/ ${value.toInt()}"
                }
            }
            chart.xAxis.textColor = axisColor
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
private fun ReservationStatusChart(statusData: Map<String, Int>) {
    if (statusData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay reservas para comparar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val barColors = listOf(
        MaterialTheme.colorScheme.tertiary.toArgb(),
        MaterialTheme.colorScheme.secondary.toArgb(),
        MaterialTheme.colorScheme.primary.toArgb(),
        MaterialTheme.colorScheme.error.toArgb()
    )

    val entries = statusData.entries.mapIndexed { index, entry ->
        BarEntry(index.toFloat(), entry.value.toFloat())
    }
    val labels = statusData.keys.toList()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
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
            }
        },
        update = { chart ->
            val dataSet = BarDataSet(entries, "Reservas").apply {
                colors = barColors
                valueTextColor = axisColor
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String = value.toInt().toString()
                }
            }

            chart.axisLeft.textColor = axisColor
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.textColor = axisColor
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.data = BarData(dataSet).apply { barWidth = 0.5f }
            chart.setFitBars(true)
            chart.invalidate()
        }
    )
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

private fun formatCurrency(amount: Double): String {
    val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
    val formatter = NumberFormat.getNumberInstance(peruLocale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "S/ " + formatter.format(amount)
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val peruLocale = Locale.Builder().setLanguage("es").setRegion("PE").build()
    return SimpleDateFormat("dd MMM yyyy", peruLocale).format(Date(timestamp))
}
