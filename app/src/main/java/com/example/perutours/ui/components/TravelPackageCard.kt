package com.example.perutours.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.perutours.data.remote.DestinationDto

@Composable
fun TravelPackageCard(
    travel: DestinationDto,
    onDetails: () -> Unit,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box {
            TravelPhoto(travel.imageUrl, travel.title, Modifier.fillMaxWidth().height(210.dp))
            if (travel.category.isNotBlank()) {
                Surface(Modifier.align(Alignment.TopStart).padding(14.dp),
                    shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
                    Text(travel.category, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            if (onToggleFavorite != null) {
                FilledIconButton(onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary)) {
                    Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritos: " + travel.title
                        else "Guardar favorito: " + travel.title)
                }
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(travel.title, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                val rating = travel.rating.toDoubleOrNull()
                if (rating != null && rating > 0) {
                    Spacer(Modifier.width(12.dp))
                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp))
                    Text(travel.rating, style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Outlined.LocationOn, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(travel.location, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Schedule, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(travel.duration, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            travel.description?.takeIf(String::isNotBlank)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Precio referencial", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(travel.price, style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary)
                }
                FilledTonalButton(onClick = onDetails) { Text("Ver destino") }
            }
            Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Solicitar cotización")
            }
        }
    }
}
