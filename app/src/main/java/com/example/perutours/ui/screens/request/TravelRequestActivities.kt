package com.example.perutours.ui.screens.request

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.example.perutours.ui.components.SectionHeading

/** Selección explícita de intereses; disponibilidad e importe los confirma el agente. */
@Composable
internal fun RequestedActivities(
    state: TravelRequestUiState,
    onToggle: (String) -> Unit,
    onRetry: () -> Unit
) {
    if (state.isLoadingPackage ||
        (state.catalogDestination == null && !state.isLoadingActivities && !state.activityLoadFailed)
    ) return
    SectionHeading(
        title = "Actividades opcionales",
        supportingText = "Marca lo que te gustaría hacer. El agente confirmará disponibilidad y precio en tu propuesta."
    )
    if (state.optionalActivityLimit == 0) {
        Text(
            "Este paquete ya incluye el máximo de servicios. Para cambiar la combinación, " +
                "solicita un viaje personalizado desde el destino.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }
    when {
        state.isLoadingActivities -> Text("Buscando actividades…")
        state.activityLoadFailed -> TextButton(onClick = onRetry, enabled = !state.isSaving) {
            Text("Reintentar búsqueda de actividades")
        }
        state.availableActivities.isEmpty() -> Text(
            "No hay actividades adicionales para este destino. Puedes pedir algo especial en observaciones.",
            style = MaterialTheme.typography.bodySmall
        )
        else -> state.availableActivities.forEach { activity ->
            val checked = activity.id in state.requestedServiceIds
            val enabled = !state.isSaving &&
                (checked || state.requestedServiceIds.size < state.optionalActivityLimit)
            Row(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = { onToggle(activity.id) }
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
                Column(modifier = Modifier.weight(1f)) {
                    Text(activity.name, fontWeight = FontWeight.SemiBold)
                    if (activity.description.isNotBlank()) {
                        Text(activity.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
