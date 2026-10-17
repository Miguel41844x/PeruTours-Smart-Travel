package com.example.perutours.ui.screens.quotation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun AddCustomServiceDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, cost: Double, quantity: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tours") }
    var costText by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text("Añadir servicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Úsalo cuando el servicio no esté disponible en el catálogo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Nombre del servicio") },
                    placeholder = { Text("Ej. Vuelo sobre las Líneas de Nazca") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                        error = null
                    },
                    label = { Text("Categoría") },
                    placeholder = { Text("Tours, alojamiento, transporte...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costText,
                        onValueChange = {
                            costText = it.filter { character -> character.isDigit() || character == '.' }
                            error = null
                        },
                        label = { Text("Costo S/") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = it.filter(Char::isDigit).take(2)
                            error = null
                        },
                        label = { Text("Cantidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull()
                    val quantity = quantityText.toIntOrNull()
                    when {
                        name.isBlank() -> error = "Ingresa el nombre del servicio."
                        category.isBlank() -> error = "Ingresa una categoría."
                        cost == null || cost <= 0 -> error = "Ingresa un costo mayor que cero."
                        quantity == null || quantity !in 1..99 -> error = "Ingresa una cantidad válida."
                        else -> onConfirm(name.trim(), category.trim(), cost, quantity)
                    }
                }
            ) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
