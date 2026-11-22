package com.example.perutours.ui.screens.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.perutours.data.model.FavoriteItem
import com.example.perutours.data.model.FavoriteType
import com.example.perutours.ui.components.*
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onExplore: () -> Unit,
    onOpenDestination: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }
    Scaffold(
        topBar = { PeruToursTopBar("Mis favoritos", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    TravelPageIntro("Tu próxima escapada",
                        "Guarda lo que te inspira y consúltalo incluso sin conexión.")
                    OutlinedTextField(value = state.query, onValueChange = viewModel::search,
                        label = { Text("Buscar en favoritos") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Outlined.Search, null) }, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "Todos", FavoriteType.DESTINATION to "Destinos",
                            FavoriteType.PACKAGE to "Paquetes").forEach { (type, label) ->
                            FilterChip(selected = state.type == type, onClick = { viewModel.filter(type) },
                                label = { Text(label) })
                        }
                    }
                }
            }
            if (state.loading) item { CircularProgressIndicator() }
            else if (state.visibleItems.isEmpty()) item {
                TravelEmptyState(
                    title = if (state.items.isEmpty()) "Aquí empieza tu lista de viajes" else "No encontramos ese favorito",
                    description = if (state.items.isEmpty())
                        "Toca el corazón de un destino o paquete para guardarlo aquí."
                    else "Prueba otro nombre o cambia el filtro.",
                    icon = Icons.Default.Favorite,
                    actionLabel = "Explorar viajes", onAction = onExplore)
            }
            items(state.visibleItems, key = FavoriteItem::key) { item ->
                FavoriteCard(item, busy = item.key in state.busyKeys,
                    onRemove = { viewModel.toggle(item) },
                    onOpen = { onOpenDestination(item.destinationId) })
            }
            if (state.items.isNotEmpty()) item {
                Text("Esta es tu selección guardada. Al solicitar un viaje confirmaremos el precio y la disponibilidad.",
                    modifier = Modifier.widthIn(max = 760.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun FavoriteCard(item: FavoriteItem, busy: Boolean, onRemove: () -> Unit, onOpen: () -> Unit) {
    var expanded by rememberSaveable(item.key) { mutableStateOf(false) }
    Card(Modifier.widthIn(max = 760.dp).fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Box {
            TravelPhoto(item.imageUrl, item.title, Modifier.fillMaxWidth().height(200.dp))
            FilledIconButton(onClick = onRemove, enabled = !busy,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Favorite, "Quitar de favoritos: " + item.title)
            }
        }
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (item.type == FavoriteType.PACKAGE) "PAQUETE" else "DESTINO",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(item.title, style = MaterialTheme.typography.titleLarge)
            Text(listOf(item.location, item.duration).filter(String::isNotBlank).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.description.isNotBlank()) {
                Text(item.description, maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Ver menos" else "Leer descripción completa")
                }
            }
            if (item.priceLabel.isNotBlank()) {
                Text(item.priceLabel + " · precio guardado",
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
                Text("Consultar destino actualizado")
            }
        }
    }
}
