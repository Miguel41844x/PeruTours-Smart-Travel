package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import com.example.perutours.data.model.Quotation
import com.example.perutours.ui.components.PeruToursTopBar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class QuotationFilter(val label: String) {
    ALL("Todas"),
    PENDING("Por responder"),
    ACCEPTED("Aceptadas"),
    CLOSED("Cerradas")
}

@Composable
fun CustomerQuotationsScreen(
    onNavigateBack: () -> Unit,
    onQuotationSelected: (String) -> Unit,
    viewModel: CustomerQuotationViewModel = koinViewModel()
) {
    val state by viewModel.quotationsState.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf(QuotationFilter.ALL) }

    LaunchedEffect(Unit) { viewModel.loadQuotations() }

    Scaffold(
        topBar = { PeruToursTopBar(title = "Mis cotizaciones", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val currentState = state) {
            CustomerQuotationsUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is CustomerQuotationsUiState.Error -> ErrorQuotations(
                message = currentState.message,
                onRetry = viewModel::loadQuotations,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )

            is CustomerQuotationsUiState.Success -> {
                val visibleQuotations = currentState.quotations.filter { quotation ->
                    when (selectedFilter) {
                        QuotationFilter.ALL -> true
                        QuotationFilter.PENDING -> Quotation.canClientRespond(quotation.status)
                        QuotationFilter.ACCEPTED -> quotation.status == Quotation.STATUS_ACCEPTED
                        QuotationFilter.CLOSED -> quotation.status == Quotation.STATUS_CANCELLED
                    }
                }

                if (currentState.quotations.isEmpty()) {
                    EmptyQuotations(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Revisa las propuestas de nuestros agentes y responde cuando estés listo.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    QuotationFilter.entries.forEach { filter ->
                                        FilterChip(
                                            selected = selectedFilter == filter,
                                            onClick = { selectedFilter = filter },
                                            label = { Text(filter.label) },
                                            leadingIcon = if (selectedFilter == filter) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }

                        if (visibleQuotations.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay cotizaciones en esta categoría.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 36.dp)
                                )
                            }
                        }

                        items(
                            items = visibleQuotations,
                            key = Quotation::id
                        ) { quotation ->
                            CustomerQuotationCard(
                                quotation = quotation,
                                onClick = { onQuotationSelected(quotation.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 720.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyQuotations(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).size(36.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Aún no tienes cotizaciones", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Cuando un agente prepare una propuesta, aparecerá aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun ErrorQuotations(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No pudimos cargar tus cotizaciones", style = MaterialTheme.typography.titleMedium)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        Button(onClick = onRetry) { Text("Intentar de nuevo") }
    }
}

@Composable
private fun CustomerQuotationCard(
    quotation: Quotation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quotation.destination,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Preparada por ${quotation.agentName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(0.dp))
                QuotationStatusPill(status = quotation.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatQuotationAmount(quotation.totalAmount, quotation.currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Válida hasta ${quotation.validityDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Ver detalle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun QuotationStatusPill(status: String) {
    val (container, content) = when (status) {
        Quotation.STATUS_ACCEPTED -> Color(0xFFD1FAE5) to Color(0xFF047857) // Verde
        Quotation.STATUS_OBSERVED -> Color(0xFFFFEDD5) to Color(0xFFC2410C) // Naranja
        Quotation.STATUS_CANCELLED -> Color(0xFFF1F5F9) to Color(0xFF64748B) // Gris
        Quotation.STATUS_QUOTED -> Color(0xFFE0F2FE) to Color(0xFF0369A1) // Azul
        else -> {
            when {
                status.contains("Aceptad", ignoreCase = true) -> Color(0xFFD1FAE5) to Color(0xFF047857)
                status.contains("Observad", ignoreCase = true) || status.contains("Cambios", ignoreCase = true) -> Color(0xFFFFEDD5) to Color(0xFFC2410C)
                status.contains("Cancelad", ignoreCase = true) -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                else -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
            }
        }
    }
    val label = when {
        status.equals(Quotation.STATUS_OBSERVED, ignoreCase = true) || status.contains("Observad", ignoreCase = true) -> "Cambios solicitados"
        status.equals(Quotation.STATUS_QUOTED, ignoreCase = true) || status.equals("Cotizado", ignoreCase = true) -> "Cotizado"
        else -> status
    }

    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

internal fun formatQuotationAmount(amount: Double, currency: String): String {
    val number = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
    return if (currency.equals("S/", ignoreCase = true) || currency.equals("PEN", ignoreCase = true)) {
        "S/ $number"
    } else {
        "\$$number $currency"
    }
}

internal fun formatQuotationCreatedAt(value: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-PE")).format(Date(value))
