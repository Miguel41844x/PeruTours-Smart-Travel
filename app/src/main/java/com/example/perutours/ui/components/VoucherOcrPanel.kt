package com.example.perutours.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perutours.domain.payment.VoucherAssessment
import com.example.perutours.domain.payment.VoucherAssessmentStatus
import com.example.perutours.domain.payment.VoucherValidator
import com.example.perutours.ui.screens.payment.VoucherOcrState
import com.example.perutours.ui.screens.quotation.formatQuotationAmount
import java.util.Locale

@Composable
internal fun VoucherOcrPanel(
    state: VoucherOcrState,
    expectedAmount: Double?,
    expectedCurrency: String,
    modifier: Modifier = Modifier,
    onRead: (() -> Unit)? = null,
    assessment: VoucherAssessment? = null
) {
    if (state == VoucherOcrState.Idle && onRead == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text("Lectura del comprobante", style = MaterialTheme.typography.titleSmall)
            val review = assessment ?: (state as? VoucherOcrState.Read)?.let {
                VoucherValidator.assess(it.result, expectedAmount, expectedCurrency)
            }
            if (review != null) {
                val reviewColor = when (review.status) {
                    VoucherAssessmentStatus.INCONSISTENT -> MaterialTheme.colorScheme.errorContainer
                    VoucherAssessmentStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.secondaryContainer
                    VoucherAssessmentStatus.CONSISTENT -> MaterialTheme.colorScheme.primaryContainer
                }
                val onReviewColor = when (review.status) {
                    VoucherAssessmentStatus.INCONSISTENT -> MaterialTheme.colorScheme.onErrorContainer
                    VoucherAssessmentStatus.NEEDS_REVIEW -> MaterialTheme.colorScheme.onSecondaryContainer
                    VoucherAssessmentStatus.CONSISTENT -> MaterialTheme.colorScheme.onPrimaryContainer
                }
                Surface(
                    color = reviewColor,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(when (review.status) {
                            VoucherAssessmentStatus.CONSISTENT -> "Sin inconsistencias detectadas"
                            VoucherAssessmentStatus.INCONSISTENT -> "Hay datos que debes corregir"
                            VoucherAssessmentStatus.NEEDS_REVIEW -> "Requiere revisión manual"
                        }, style = MaterialTheme.typography.titleSmall,
                            color = onReviewColor)
                        review.issues.filter { it.highlighted }.forEach {
                            Text(it.message, style = MaterialTheme.typography.bodySmall,
                                color = onReviewColor)
                        }
                    }
                }
            }
            when (state) {
                VoucherOcrState.Idle -> Unit
                VoucherOcrState.Reading -> Text("Leyendo la imagen…")
                VoucherOcrState.NoData -> Text(
                    "No se reconoció importe, operación, fecha ni hora. La imagen no contiene datos de pago legibles.",
                    color = MaterialTheme.colorScheme.error
                )
                VoucherOcrState.Unavailable -> Text(
                    "No pudimos completar la lectura. Reintenta el análisis de la imagen."
                )
                is VoucherOcrState.Read -> {
                    val result = state.result
                    result.amount?.let { amount ->
                        val formatted = formatQuotationAmount(amount.value.toDouble(), amount.currency)
                        Text("Importe detectado: $formatted")
                        if (expectedAmount != null && expectedAmount.isFinite()) {
                            val matches = amount.matches(expectedAmount, expectedCurrency)
                            Text(
                                text = if (matches) "Coincide con el importe de la reserva."
                                else "El importe o la moneda difiere de la reserva. Revisa el comprobante.",
                                color = if (matches) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }
                    } ?: Text("Importe por verificar en la imagen.")
                    result.operationNumber?.let { Text("N.º de operación detectado: $it") }
                        ?: Text("N.º de operación por verificar en la imagen.")
                    result.transactionDate?.let { date ->
                        Text("Fecha detectada: " + String.format(
                            Locale.ROOT, "%02d/%02d/%04d", date.day, date.month, date.year
                        ))
                    } ?: Text("Fecha por verificar en la imagen.")
                    result.transactionTime?.let { time ->
                        val value = if (time.second == null) {
                            String.format(Locale.ROOT, "%02d:%02d", time.hour, time.minute)
                        } else {
                            String.format(Locale.ROOT, "%02d:%02d:%02d", time.hour, time.minute, time.second)
                        }
                        Text("Hora detectada: " + value)
                    } ?: Text("Hora por verificar en la imagen.")
                }
            }
            if (state is VoucherOcrState.Read || state == VoucherOcrState.Unavailable) {
                Text("Esta lectura comprueba los datos visibles. La recepción del dinero se confirma en la cuenta de la agencia.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val partialReading = state is VoucherOcrState.Read && (
                state.result.amount == null || state.result.operationNumber == null ||
                    state.result.transactionDate == null || state.result.transactionTime == null ||
                    review?.hasBlockingIssues == true
                )
            if (onRead != null &&
                (state == VoucherOcrState.Idle || state == VoucherOcrState.NoData ||
                    state == VoucherOcrState.Unavailable || partialReading)
            ) {
                TextButton(onClick = onRead) {
                    Text(if (state == VoucherOcrState.Idle) "Leer comprobante" else "Reintentar lectura")
                }
            }
        }
    }
}
