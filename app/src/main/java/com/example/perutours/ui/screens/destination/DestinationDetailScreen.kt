package com.example.perutours.ui.screens.destination

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.perutours.data.model.TourPackage
import com.example.perutours.data.repository.PointsOfInterestRepository
import org.koin.androidx.compose.koinViewModel

/**
 * Detalle del destino para el cliente.
 *
 * Muestra la información del lugar, sus paquetes publicados y los puntos
 * de interés cercanos obtenidos de la API externa de Geoapify.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailScreen(
    destinationId: String,
    onNavigateBack: () -> Unit,
    onRequestQuote: (TourPackage) -> Unit = {},
    viewModel: DestinationDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(destinationId) { viewModel.load(destinationId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.destination?.name ?: "Destino") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::refreshNearbyPlaces,
                        enabled = !uiState.arePlacesLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar lugares")
                    }
                }
            )
        }
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
                Spacer(Modifier.height(12.dp))
                Text("Cargando destino...")
            }

            uiState.errorMessage != null -> Text(
                text = uiState.errorMessage.orEmpty(),
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            )

            else -> {
                val destination = uiState.destination ?: return@Scaffold
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        if (destination.coverImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = destination.coverImageUrl,
                                contentDescription = destination.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(12.dp))
                        }

                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${destination.department}, Perú",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (destination.summary.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = destination.summary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (destination.categories.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = destination.categories.joinToString(" · "),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (uiState.packages.isNotEmpty()) {
                        item {
                            SectionTitle("Paquetes disponibles")
                        }
                        items(uiState.packages, key = { it.id }) { tourPackage ->
                            PackageSummaryCard(tourPackage = tourPackage)
                        }
                    }

                    item {
                        SectionTitle("Qué visitar cerca")
                    }

                    when {
                        uiState.arePlacesLoading -> item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Buscando lugares cercanos...")
                            }
                        }

                        uiState.placesErrorMessage != null -> item {
                            Text(
                                text = uiState.placesErrorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        uiState.places.isEmpty() -> item {
                            Text(
                                text = "Este destino aún no tiene coordenadas validadas " +
                                    "para mostrar lugares cercanos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> items(uiState.places, key = { it.id }) { place ->
                            NearbyPlaceCard(place = place)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PackageSummaryCard(tourPackage: TourPackage) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = tourPackage.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (tourPackage.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tourPackage.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tourPackage.durationLabel(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = tourPackage.priceLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NearbyPlaceCard(place: PointsOfInterestRepository.NearbyPlace) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (place.categoryLabel.isNotBlank()) {
                    Text(
                        text = place.categoryLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (place.address.isNotBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDistance(place.distanceMeters),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDistance(distanceMeters: Double): String =
    if (distanceMeters >= 1_000) {
        "%.1f km".format(distanceMeters / 1_000).replace('.', ',')
    } else {
        "${distanceMeters.toInt()} m"
    }
