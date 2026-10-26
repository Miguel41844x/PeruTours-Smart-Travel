package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.example.perutours.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.perutours.data.model.Payment
import com.example.perutours.data.repository.PaymentValidator
import com.example.perutours.ui.components.PeruToursTopBar
import com.example.perutours.ui.screens.quotation.formatQuotationAmount
import com.example.perutours.ui.theme.PeruGold40
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * HU07: lista de comprobantes 'Pendiente' asignados al agente.
 */
@Composable
fun AgentPaymentsScreen(
    onNavigateBack: () -> Unit,
    onPaymentSelected: (String) -> Unit,
    viewModel: AgentPaymentViewModel = koinViewModel()
) {
    val uiState by viewModel.paymentsState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadPendingPayments()
    }

    Scaffold(
        topBar = {
            PeruToursTopBar(title = "Comprobantes por revisar", onBack = onNavigateBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            uiState.errorMessage != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            uiState.payments.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.infoMessage ?: "No hay comprobantes pendientes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(uiState.payments, key = { payment -> payment.id }) { payment ->
                    PaymentListItem(
                        payment = payment,
                        clientName = uiState.clientNames[payment.clientId].orEmpty(),
                        onClick = { onPaymentSelected(payment.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentListItem(
    payment: Payment,
    clientName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp).size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clientName.ifBlank { "Cliente" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${payment.method} · Enviado el ${formatPaymentDateTime(payment.submittedAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatQuotationAmount(payment.amount, payment.currency),
                style = MaterialTheme.typography.titleSmall,
                color = PeruGold40
            )
        }
    }
}

/**
 * HU07: detalle del comprobante con las acciones del agente: aprobar
 * el pago (reserva → Pagado) o rechazarlo con un motivo obligatorio.
 */
@Composable
fun AgentPaymentDetailScreen(
    paymentId: String,
    onNavigateBack: () -> Unit,
    viewModel: AgentPaymentViewModel = koinViewModel()
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(paymentId) {
        viewModel.loadPaymentDetail(paymentId)
    }

    if (showRejectDialog) {
        RejectPaymentDialog(
            isSubmitting = uiState.isSubmitting,
            onConfirm = { reason ->
                showRejectDialog = false
                viewModel.rejectPayment(reason)
            },
            onDismiss = { showRejectDialog = false }
        )
    }

    Scaffold(
        topBar = { PeruToursTopBar(title = "Revisar comprobante", onBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val payment = uiState.payment
        when {
            uiState.isLoading -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            payment == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "No se encontró el pago.",
                    color = MaterialTheme.colorScheme.error
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Comprobante capturado por el cliente.
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                        .height(260.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (payment.voucherUrl.isNotBlank()) {
                        AsyncImage(
                            model = payment.voucherUrl,
                            contentDescription = "Comprobante de pago",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Sin comprobante adjunto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatQuotationAmount(payment.amount, payment.currency),
                            style = MaterialTheme.typography.headlineSmall,
                            color = PeruGold40
                        )
                        DetailRow(
                            label = "Cliente",
                            value = uiState.clientName?.takeIf(String::isNotBlank)
                                ?: payment.clientId
                        )
                        DetailRow(label = "Método de pago", value = payment.method)
                        DetailRow(
                            label = "Enviado el",
                            value = formatPaymentDateTime(payment.submittedAtMillis)
                        )
                        uiState.reservation?.let { reservation ->
                            DetailRow(label = "Destino", value = reservation.destination)
                            DetailRow(
                                label = "Ruta",
                                value = "${reservation.originCity.ifBlank { "Origen no disponible" }} → ${reservation.destination}"
                            )
                            DetailRow(
                                label = "Salida",
                                value = formatPaymentDateTime(reservation.departureAtMillis)
                            )
                        }
                    }
                }

                if (payment.status == Payment.STATUS_PENDING) {
                    Button(
                        onClick = viewModel::approvePayment,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PeruValley)
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Aprobar pago", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showRejectDialog = true },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Rechazar comprobante", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    PaymentResolutionCard(payment = payment)
                }

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                uiState.resolutionMessage?.let { message ->
                    Text(
                        text = message,
                        color = PeruValleyDark,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PaymentResolutionCard(payment: Payment) {
    val approved = payment.status == Payment.STATUS_APPROVED
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (approved) PeruValleyContainer else MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (approved) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (approved) PeruValley else MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (approved) "Pago aprobado" else "Pago rechazado",
                    fontWeight = FontWeight.Bold,
                    color = if (approved) PeruValleyDark else MaterialTheme.colorScheme.onErrorContainer
                )
                if (!approved && payment.rejectionReason.isNotBlank()) {
                    Text(
                        text = payment.rejectionReason,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    Text(
                        text = "La reserva del cliente quedó en estado Pagado.",
                        fontSize = 12.sp,
                        color = PeruValleyDark
                    )
                }
            }
        }
    }
}

/**
 * HU07: diálogo de rechazo; el motivo es obligatorio y debe tener al
 * menos 10 caracteres para poder enviarse.
 */
@Composable
private fun RejectPaymentDialog(
    isSubmitting: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val reasonIsValid = PaymentValidator.isValidRejectionReason(reason)

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Rechazar comprobante") },
        text = {
            Column {
                Text(
                    text = "Explica al cliente por qué el comprobante no es válido (mínimo ${PaymentValidator.MIN_REJECTION_LENGTH} caracteres).",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Motivo del rechazo") },
                    isError = reason.isNotBlank() && !reasonIsValid,
                    supportingText = {
                        if (reason.isNotBlank() && !reasonIsValid) {
                            Text("Faltan caracteres para llegar a ${PaymentValidator.MIN_REJECTION_LENGTH}.")
                        }
                    },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason) },
                enabled = reasonIsValid && !isSubmitting
            ) {
                Text("Rechazar pago")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatPaymentDateTime(value: Long): String {
    if (value <= 0L) return "No disponible"
    return SimpleDateFormat(
        "d MMM yyyy, HH:mm",
        Locale.forLanguageTag("es-PE")
    ).format(Date(value))
}
