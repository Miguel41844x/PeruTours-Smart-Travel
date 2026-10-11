package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.components.SectionHeading
import com.example.perutours.ui.screens.quotation.components.AddCustomServiceDialog
import com.example.perutours.ui.screens.quotation.components.FinancialSummaryCard
import com.example.perutours.ui.screens.quotation.components.ServiceCatalogItemCard

@Composable
fun CreateQuotationScreen(
    viewModel: QuotationViewModel,
    onNavigateBack: () -> Unit,
    onQuotationCreated: (String) -> Unit
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val catalogServices by viewModel.catalogServices.collectAsState()
    val catalogLoading by viewModel.catalogLoading.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }
    val baseCost = formState.selectedServices.sumOf { it.unitCost * it.quantity }
    val totalAmount = Math.round(
        baseCost * (1.0 + formState.marginPercent / 100.0) * 100.0
    ) / 100.0

    LaunchedEffect(Unit) { viewModel.loadCatalogServices() }
    LaunchedEffect(uiState) {
        if (uiState is QuotationUiState.Success) {
            onQuotationCreated((uiState as QuotationUiState.Success).quotation.id)
        }
    }

    Scaffold(
        topBar = {
            PeruToursTopBar(
                title = "Crear cotización",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir servicio personalizado")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = viewModel::saveQuotation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 900.dp)
                            .height(52.dp),
                        enabled = uiState !is QuotationUiState.Loading
                    ) {
                        if (uiState is QuotationUiState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text("Guardando...", modifier = Modifier.padding(start = 10.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar y notificar al cliente")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is QuotationUiState.Error) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 900.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = (uiState as QuotationUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            item {
                RequestSummary(
                    clientName = formState.clientName,
                    clientEmail = formState.clientEmail,
                    destination = formState.destination,
                    travelerCount = formState.travelerCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 900.dp)
                )
            }

            item {
                FinancialSummaryCard(
                    baseCost = baseCost,
                    marginPercent = formState.marginPercent,
                    onMarginChange = viewModel::setMarginPercent,
                    totalAmount = totalAmount,
                    travelerCount = formState.travelerCount,
                    modifier = Modifier.widthIn(max = 900.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 900.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    SectionHeading(
                        title = "Servicios",
                        supportingText = "Toca un servicio para incluirlo o quitarlo."
                    )
                    Text(
                        text = "${formState.selectedServices.size} seleccionados",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (catalogLoading) {
                item {
                    CircularProgressIndicator(modifier = Modifier.padding(28.dp))
                }
            } else {
                items(items = catalogServices, key = { service -> service.id }) { service ->
                    val isSelected = formState.selectedServices.any { it.id == service.id }
                    ServiceCatalogItemCard(
                        catalogService = service,
                        isSelected = isSelected,
                        onToggle = { viewModel.addServiceFromCatalog(service) },
                        modifier = Modifier.widthIn(max = 900.dp)
                    )
                }
            }
        }

        if (showCustomDialog) {
            AddCustomServiceDialog(
                onDismiss = { showCustomDialog = false },
                onConfirm = { name, category, cost, quantity ->
                    viewModel.addCustomService(name, category, cost, quantity)
                    showCustomDialog = false
                }
            )
        }
    }
}

@Composable
private fun RequestSummary(
    clientName: String,
    clientEmail: String,
    destination: String,
    travelerCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Solicitud del cliente", style = MaterialTheme.typography.titleMedium)
            Text(
                text = clientName,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = clientEmail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(destination, style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$travelerCount viajeros", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
