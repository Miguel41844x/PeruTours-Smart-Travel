package com.example.perutours.ui.screens.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.SalesAnalytics
import com.example.perutours.ui.screens.sales.components.DestinationRanking
import com.example.perutours.ui.screens.sales.components.MonthlyRevenueLineChart
import com.example.perutours.ui.screens.sales.components.QuotationStatusBarChart
import org.koin.androidx.compose.koinViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDashboardScreen(
    onLogout: () -> Unit,
    viewModel: SalesDashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ventas y cotizaciones")
                        Text(
                            text = "Panel gerencial",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar estadísticas")
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        DashboardContent(
            uiState = uiState,
            onRetry = viewModel::refresh,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: SalesDashboardUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading && !uiState.analytics.hasData) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (uiState.isLoading) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }

        uiState.errorMessage?.let { message ->
            item {
                ErrorPanel(message = message, onRetry = onRetry)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Resumen comercial",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Actualizado ${formatUpdatedAt(uiState.analytics.updatedAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { SalesSummary(analytics = uiState.analytics) }

        item {
            DashboardSection(
                title = "Cotizaciones por estado",
                supportingText = "Distribución actual de las propuestas enviadas a clientes."
            ) {
                QuotationStatusBarChart(metrics = uiState.analytics.quotationStatuses)
            }
        }

        item {
            DashboardSection(
                title = "Ingresos aprobados",
                supportingText = "Pagos confirmados durante los últimos seis meses."
            ) {
                MonthlyRevenueLineChart(
                    metrics = uiState.analytics.monthlyRevenue,
                    currency = uiState.analytics.currency
                )
            }
        }

        item {
            DashboardSection(
                title = "Destinos más solicitados",
                supportingText = "Ranking construido con las solicitudes de viaje registradas."
            ) {
                DestinationRanking(destinations = uiState.analytics.topDestinations)
            }
        }
    }
}

@Composable
private fun SalesSummary(analytics: SalesAnalytics) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val wideLayout = maxWidth >= 720.dp
        SummaryCards(analytics = analytics, singleRow = wideLayout)
    }
}

@Composable
private fun SummaryCards(analytics: SalesAnalytics, singleRow: Boolean) {
    val metrics = listOf(
        SummaryMetric(
            label = "Cotizaciones",
            value = analytics.totalQuotations.toString(),
            icon = Icons.Default.RequestQuote,
            accent = MaterialTheme.colorScheme.primary
        ),
        SummaryMetric(
            label = "Ventas aprobadas",
            value = analytics.approvedSales.toString(),
            icon = Icons.Default.CheckCircle,
            accent = MaterialTheme.colorScheme.tertiary
        ),
        SummaryMetric(
            label = "Ingresos",
            value = formatCurrency(analytics.totalRevenue, analytics.currency),
            icon = Icons.Default.Payments,
            accent = MaterialTheme.colorScheme.secondary
        ),
        SummaryMetric(
            label = "Conversión",
            value = "${analytics.conversionRate.toInt()}%",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            accent = MaterialTheme.colorScheme.primary
        )
    )

    if (singleRow) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.forEach { metric ->
                MetricCard(
                    label = metric.label,
                    value = metric.value,
                    icon = metric.icon,
                    accent = metric.accent,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { metric ->
                        MetricCard(
                            label = metric.label,
                            value = metric.value,
                            icon = metric.icon,
                            accent = metric.accent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(116.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardSection(
    title: String,
    supportingText: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            content()
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
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
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

private fun formatCurrency(amount: Double, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(PERU_LOCALE).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    val prefix = if (currency.equals("PEN", ignoreCase = true)) "S/ " else "$currency "
    return prefix + formatter.format(amount)
}

private fun formatUpdatedAt(timestamp: Long): String {
    if (timestamp <= 0L) return "pendiente"
    return SimpleDateFormat("dd MMM, HH:mm", PERU_LOCALE).format(Date(timestamp))
}

private val PERU_LOCALE: Locale = Locale.Builder()
    .setLanguage("es")
    .setRegion("PE")
    .build()

private data class SummaryMetric(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color
)
