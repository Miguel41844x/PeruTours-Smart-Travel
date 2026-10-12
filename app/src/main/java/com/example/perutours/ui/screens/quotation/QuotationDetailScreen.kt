package com.example.perutours.ui.screens.quotation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationObservation
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.components.SectionHeading

@Composable
fun QuotationDetailScreen(
    quotation: Quotation,
    onNavigateBack: () -> Unit,
    onAcceptQuotation: (String) -> Unit = {},
    onObserveQuotation: (String, String) -> Unit = { _, _ -> },
    onCancelQuotation: (String) -> Unit = {},
    observations: List<QuotationObservation> = emptyList(),
    showCustomerActions: Boolean = true,
    showAgentRevisionAction: Boolean = false,
    isSavingResponse: Boolean = false,
    responseMessage: String? = null,
    responseIsError: Boolean = false,
    onViewReservation: (String) -> Unit = {},
    onReviseQuotation: (String) -> Unit = {},
    onAgentMenu: () -> Unit = {}
) {
    var showAcceptConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var showObservationDialog by remember { mutableStateOf(false) }
    val canRespond = Quotation.canClientRespond(quotation.status)
    val acceptanceError = QuotationResponseValidator.validateAcceptance(quotation)

    if (showAcceptConfirmation) {
        ConfirmationDialog(
            title = "Aceptar cotización",
            message = "Confirmaremos esta propuesta y crearemos tu reserva.",
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
            message = "La propuesta quedará cancelada y ya no podrás aceptarla.",
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
        topBar = { PeruToursTopBar(title = "Detalle de cotización", onBack = onNavigateBack) },
        bottomBar = {
            if (showCustomerActions && canRespond) {
                CustomerResponseBar(
                    enabled = !isSavingResponse,
                    acceptEnabled = !isSavingResponse && acceptanceError == null,
                    acceptanceError = acceptanceError,
                    onAccept = { showAcceptConfirmation = true },
                    onRequestChanges = { showObservationDialog = true },
                    onCancel = { showCancelConfirmation = true }
                )
            } else if (showAgentRevisionAction) {
                AgentRevisionBar(
                    isObserved = quotation.status == Quotation.STATUS_OBSERVED,
                    onRevise = { onReviseQuotation(quotation.id) },
                    onAgentMenu = onAgentMenu
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 20.dp,
                vertical = 18.dp
            )
        ) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
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
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Cotización #${quotation.id.takeLast(6).uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            QuotationStatusPill(status = quotation.status)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${quotation.travelerCount} ${if (quotation.travelerCount == 1) "viajero" else "viajeros"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = formatQuotationAmount(quotation.totalAmount, quotation.currency),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Preparada por ${quotation.agentName} · Válida hasta ${quotation.validityDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (
                showAgentRevisionAction &&
                quotation.status == Quotation.STATUS_OBSERVED &&
                quotation.latestObservation.isNotBlank()
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Indicación del turista",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = quotation.latestObservation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (responseMessage != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        color = if (responseIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer
                        },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = responseMessage,
                            color = if (responseIsError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            if (
                showCustomerActions &&
                quotation.status == Quotation.STATUS_ACCEPTED &&
                quotation.reservationId.isNotBlank()
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reserva confirmada",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "El paquete aceptado ya está disponible en tus reservas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            OutlinedButton(
                                onClick = { onViewReservation(quotation.reservationId) },
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Icon(Icons.Default.Luggage, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Ver reserva")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeading(
                    title = "Servicios incluidos",
                    supportingText = "${quotation.services.size} elementos en la propuesta",
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                )
            }

            items(items = quotation.services, key = { service -> service.id }) { service ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(service.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${service.category} · Cantidad ${service.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatQuotationAmount(service.subtotal, quotation.currency),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total del paquete", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatQuotationAmount(quotation.totalAmount, quotation.currency),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Condiciones", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = quotation.conditions.ifBlank { "No se indicaron condiciones adicionales." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (observations.isNotEmpty()) {
                item {
                    SectionHeading(
                        title = "Cambios solicitados",
                        supportingText = "Historial de mensajes sobre esta propuesta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                    )
                }
                items(items = observations, key = { observation -> observation.id }) { observation ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                observation.authorName.ifBlank { "Cliente" },
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                observation.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentRevisionBar(
    isObserved: Boolean,
    onRevise: () -> Unit,
    onAgentMenu: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isObserved) {
                    Button(
                        onClick = onRevise,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Revisar y reenviar")
                    }
                }
                OutlinedButton(
                    onClick = onAgentMenu,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Store, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ir al menú")
                }
            }
        }
    }
}

@Composable
private fun CustomerResponseBar(
    enabled: Boolean,
    acceptEnabled: Boolean,
    acceptanceError: String?,
    onAccept: () -> Unit,
    onRequestChanges: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
            ) {
                Button(
                    onClick = onAccept,
                    enabled = acceptEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aceptar y reservar")
                }
                acceptanceError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestChanges,
                        enabled = enabled,
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Solicitar cambios")
                    }
                    TextButton(
                        onClick = onCancel,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.EventBusy, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationDialog(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Solicitar cambios") },
        text = {
            Column {
                Text(
                    text = "Explica qué debería revisar o modificar el agente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { value ->
                        if (value.length <= QuotationResponseValidator.MAX_OBSERVATION_LENGTH) {
                            message = value
                            error = null
                        }
                    },
                    label = { Text("Cambios solicitados") },
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
            ) { Text("Enviar solicitud") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
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
                    color = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Volver") } }
    )
}
