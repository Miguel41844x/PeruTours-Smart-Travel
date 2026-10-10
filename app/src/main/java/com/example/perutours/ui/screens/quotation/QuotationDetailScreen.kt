package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationObservation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationDetailScreen(
    quotation: Quotation,
    onNavigateBack: () -> Unit,
    onAcceptQuotation: (String) -> Unit = {},
    onObserveQuotation: (String, String) -> Unit = { _, _ -> },
    onCancelQuotation: (String) -> Unit = {},
    observations: List<QuotationObservation> = emptyList(),
    showCustomerActions: Boolean = true,
    isSavingResponse: Boolean = false,
    responseMessage: String? = null,
    responseIsError: Boolean = false
) {
    var showAcceptConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showObservationDialog by remember { mutableStateOf(false) }
    val canRespond = Quotation.canClientRespond(quotation.status)

    if (showAcceptConfirmation) {
        ConfirmationDialog(
            title = "Aceptar cotización",
            message = "Se confirmará el paquete y se generará tu reserva.",
            confirmLabel = "Aceptar y reservar",
            onDismiss = { showAcceptConfirmation = false },
            onConfirm = {
                showAcceptConfirmation = false
                onAcceptQuotation(quotation.id)
            }
        )
    }

    if (showCancelConfirmation) {
        ConfirmationDialog(
            title = "Cancelar cotización",
            message = "La propuesta quedará cancelada y ya no podrá aceptarse.",
            confirmLabel = "Cancelar cotización",
            destructive = true,
            onDismiss = { showCancelConfirmation = false },
            onConfirm = {
                showCancelConfirmation = false
                onCancelQuotation(quotation.id)
            }
        )
    }

    if (showObservationDialog) {
        ObservationDialog(
            onDismiss = { showObservationDialog = false },
            onSubmit = { message ->
                showObservationDialog = false
                onObserveQuotation(quotation.id, message)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cotización #${quotation.id.takeLast(6)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (showCustomerActions && canRespond) {
                CustomerResponseBar(
                    enabled = !isSavingResponse,
                    onAccept = { showAcceptConfirmation = true },
                    onObserve = { showObservationDialog = true },
                    onCancel = { showCancelConfirmation = true }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quotation.destination,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.weight(1f)
                    )
                    QuotationStatus(status = quotation.status)
                }
            }

            item {
                Text(
                    text = "Asesor turístico: ${quotation.agentName} • Vigencia: ${quotation.validityDate}",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                )
            }

            if (responseMessage != null) {
                item {
                    Surface(
                        color = if (responseIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            Color(0xFFDCFCE7)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = responseMessage,
                            color = if (responseIsError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                Color(0xFF166534)
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Servicios incluidos (${quotation.services.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = quotation.services,
                key = { service -> service.id }
            ) { service ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Cantidad: ${service.quantity} • ${service.category}",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                        Text(
                            text = "$${"%.2f".format(service.subtotal)} ${quotation.currency}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C1917), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL DEL PAQUETE",
                            color = Color(0xFF9CA3AF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "$${"%.2f".format(quotation.totalAmount)} ${quotation.currency}",
                            color = Color(0xFFFBBF24),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Condiciones comerciales",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = quotation.conditions,
                    fontSize = 11.sp,
                    color = Color(0xFF4B5563),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (observations.isNotEmpty()) {
                item {
                    Text(
                        text = "Historial de observaciones",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(
                    items = observations,
                    key = { observation -> observation.id }
                ) { observation ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = observation.authorName.ifBlank { "Cliente" },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = observation.message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerResponseBar(
    enabled: Boolean,
    onAccept: () -> Unit,
    onObserve: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Button(
                onClick = onAccept,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text("Aceptar y reservar", fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onObserve,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null)
                    Text("Observar", modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(
                    onClick = onCancel,
                    enabled = enabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null)
                    Text("Cancelar", modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

@Composable
private fun ObservationDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Observar cotización") },
        text = {
            Column {
                Text(
                    text = "Indica al agente qué debe revisar o modificar.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { value ->
                        if (value.length <= QuotationResponseValidator.MAX_OBSERVATION_LENGTH) {
                            message = value
                            error = null
                        }
                    },
                    label = { Text("Observación") },
                    supportingText = {
                        Text(error ?: "${message.length}/${QuotationResponseValidator.MAX_OBSERVATION_LENGTH}")
                    },
                    isError = error != null,
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val validationError = QuotationResponseValidator.validateObservation(message)
                    if (validationError == null) onSubmit(message.trim()) else error = validationError
                }
            ) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Volver")
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else Color(0xFF047857)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Volver")
            }
        }
    )
}

@Composable
private fun QuotationStatus(status: String) {
    val colors = when (status) {
        Quotation.STATUS_ACCEPTED -> Color(0xFFDCFCE7) to Color(0xFF166534)
        Quotation.STATUS_OBSERVED -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        Quotation.STATUS_CANCELLED -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        else -> Color(0xFFFEF3C7) to Color(0xFFB45309)
    }

    Surface(
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
