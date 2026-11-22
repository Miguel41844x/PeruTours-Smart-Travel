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
import com.example.perutours.ui.screens.payment.VoucherOcrState
import com.example.perutours.ui.screens.quotation.formatQuotationAmount

@Composable
internal fun VoucherOcrPanel(
    state: VoucherOcrState,
    expectedAmount: Double?,
    expectedCurrency: String,
    modifier: Modifier = Modifier,
    onRead: (() -> Unit)? = null
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
            when (state) {
                VoucherOcrState.Idle -> Unit
                VoucherOcrState.Reading -> Text("Leyendo la imagen…")
                VoucherOcrState.Unavailable -> Text(
                    "No pudimos leer el importe ni el número de operación. Revisa la imagen."
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
                }
            }
            if (onRead != null &&
                (state == VoucherOcrState.Idle || state == VoucherOcrState.Unavailable)
            ) {
                TextButton(onClick = onRead) {
                    Text(if (state == VoucherOcrState.Idle) "Leer comprobante" else "Reintentar lectura")
                }
            }
        }
    }
}
