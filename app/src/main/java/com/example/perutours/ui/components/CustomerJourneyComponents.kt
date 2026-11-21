package com.example.perutours.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class CustomerSection {
    HOME,
    QUOTATIONS,
    RESERVATIONS,
    TRIPS,
    CHAT
}

private data class CustomerDestination(
    val section: CustomerSection,
    val label: String,
    val icon: ImageVector
)

@Composable
fun CustomerNavigationBar(
    selected: CustomerSection,
    onHome: () -> Unit,
    onQuotations: () -> Unit,
    onReservations: () -> Unit,
    onTrips: () -> Unit,
    onChat: (() -> Unit)? = null
) {
    val destinations = listOf(
        CustomerDestination(CustomerSection.HOME, "Inicio", Icons.Default.Home),
        CustomerDestination(
            CustomerSection.QUOTATIONS,
            "Cotizaciones",
            Icons.AutoMirrored.Filled.ReceiptLong
        ),
        CustomerDestination(CustomerSection.RESERVATIONS, "Reservas", Icons.Default.Payments),
        CustomerDestination(CustomerSection.TRIPS, "Viajes", Icons.Default.Luggage)
    ) + listOfNotNull(onChat?.let {
        CustomerDestination(CustomerSection.CHAT, "Asistente", Icons.Default.SmartToy)
    })
    val callbacks = mapOf(
        CustomerSection.HOME to onHome,
        CustomerSection.QUOTATIONS to onQuotations,
        CustomerSection.RESERVATIONS to onReservations,
        CustomerSection.TRIPS to onTrips,
        CustomerSection.CHAT to (onChat ?: {})
    )

    NavigationBar {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selected == destination.section,
                onClick = callbacks.getValue(destination.section),
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) }
            )
        }
    }
}

/** Resume el expediente del viaje con las mismas etapas en todas las pantallas. */
@Composable
fun JourneyProgress(
    currentStep: Int,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf("Solicitud", "Cotización", "Pago", "Viaje")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            steps.forEachIndexed { index, label ->
                val stepNumber = index + 1
                val completed = stepNumber <= currentStep
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (index > 0) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(
                                        if (completed) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = if (completed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stepNumber.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (completed) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (index < steps.lastIndex) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(
                                        if (stepNumber < currentStep) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
