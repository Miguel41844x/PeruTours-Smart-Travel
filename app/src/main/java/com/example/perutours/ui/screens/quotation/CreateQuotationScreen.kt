package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.ui.screens.quotation.components.AddCustomServiceDialog
import com.example.perutours.ui.screens.quotation.components.FinancialSummaryCard
import com.example.perutours.ui.screens.quotation.components.ServiceCatalogItemCard

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) {
        viewModel.loadCatalogServices()
    }

    var showCustomDialog by remember { mutableStateOf(false) }

    val baseCost = formState.selectedServices.sumOf { it.unitCost * it.quantity }
    val totalAmount = Math.round(baseCost * (1.0 + formState.marginPercent / 100.0) * 100.0) / 100.0

    LaunchedEffect(uiState) {
        if (uiState is QuotationUiState.Success) {
            val quot = (uiState as QuotationUiState.Success).quotation
            onQuotationCreated(quot.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Elaborar Cotización", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Servicio Libre")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = { viewModel.saveQuotation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706)
                    ),
                    enabled = uiState !is QuotationUiState.Loading
                ) {
                    if (uiState is QuotationUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            "Registrar cotización y Notificar",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (uiState is QuotationUiState.Loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                if (uiState is QuotationUiState.Error) {
                    Text(
                        text = (uiState as QuotationUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {

                        Text(
                            text = "Datos de la solicitud",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Cliente: ${formState.clientName}",
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Correo: ${formState.clientEmail}",
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Destino: ${formState.destination}",
                            fontSize = 14.sp
                        )

                        Text(
                            text = "Viajeros: ${formState.travelerCount}",
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item {
                FinancialSummaryCard(
                    baseCost = baseCost,
                    marginPercent = formState.marginPercent,
                    onMarginChange = { viewModel.setMarginPercent(it) },
                    totalAmount = totalAmount,
                    travelerCount = formState.travelerCount
                )
            }

            item {
                Text(
                    text = "Servicios Disponibles en Catálogo (Toca para incluir):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
            }

            if (catalogLoading) {

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

            } else {

                items(catalogServices) { service ->

                    val isSelected =
                        formState.selectedServices.any {
                            it.id == service.id
                        }

                    ServiceCatalogItemCard(
                        catalogService = service,
                        isSelected = isSelected,
                        onToggle = {
                            viewModel.addServiceFromCatalog(service)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        if (showCustomDialog) {
            AddCustomServiceDialog(
                onDismiss = { showCustomDialog = false },
                onConfirm = { name, cat, cost, qty ->
                    viewModel.addCustomService(name, cat, cost, qty)
                    showCustomDialog = false
                }
            )
        }
    }
}