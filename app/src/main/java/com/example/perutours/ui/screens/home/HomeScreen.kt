package com.example.perutours.ui.screens.home

import com.example.perutours.ui.components.TravelPhoto
import com.example.perutours.ui.components.TravelPackageCard
import com.example.perutours.ui.components.TravelEmptyState
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.perutours.ui.components.TravelTopAppBar as TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import org.koin.androidx.compose.koinViewModel
import coil.compose.AsyncImage
import com.example.perutours.R
import com.example.perutours.data.model.Recommendation
import com.example.perutours.data.remote.DestinationDto
import com.example.perutours.ui.components.PeruToursBrand
import com.example.perutours.ui.components.CustomerNavigationBar
import com.example.perutours.ui.components.CustomerSection
import com.example.perutours.ui.components.ScreenHorizontalPadding
import com.example.perutours.ui.components.SectionHeading
import com.example.perutours.ui.screens.recommendation.RecommendationUiState
import com.example.perutours.ui.screens.recommendation.RecommendationViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.perutours.data.messaging.FCMTokenManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.compose.koinInject
import androidx.compose.material.icons.filled.SmartToy

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("InlinedApi")
@Composable
fun HomeScreen(
    auth: FirebaseAuth,
    navigateToTravelRequest: (DestinationDto?) -> Unit = {},
    navigateToQuotations: () -> Unit = {},
    navigateToReservations: () -> Unit = {},
    navigateToTrips: () -> Unit = {},
    navigateToChat: () -> Unit = {},
    navigateToProfile: () -> Unit = {},
    navigateToDetail: (String) -> Unit = {},
    navigateToInitial: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
    recommendationViewModel: RecommendationViewModel = koinViewModel(),
    fcmTokenManager: FCMTokenManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recommendationState by recommendationViewModel.uiState.collectAsStateWithLifecycle()

    // HU10: al volver al Home (p. ej. desde Perfil tras guardar las
    // preferencias) se reconsultan las recomendaciones para que no
    // queden desactualizadas. El caché de Firestore se invalida solo
    // si cambiaron las preferencias.
    LifecycleResumeEffect(Unit) {
        recommendationViewModel.onHomeResumed()
        onPauseOrDispose { }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showNotificationPrompt by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { navigateToQuotations() }
    )

    fun openQuotations() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (needsPermission) showNotificationPrompt = true else navigateToQuotations()
    }

    if (showNotificationPrompt) {
        AlertDialog(
            onDismissRequest = { showNotificationPrompt = false },
            title = { Text("Avisos sobre tus cotizaciones") },
            text = { Text("Activa las notificaciones para saber cuando recibas una propuesta o cambie su estado.") },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPrompt = false
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) { Text("Activar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotificationPrompt = false
                        navigateToQuotations()
                    }
                ) { Text("Ahora no") }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Tendrás que ingresar nuevamente para ver tus viajes y cotizaciones.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        coroutineScope.launch {
                            withTimeoutOrNull(3_000) { fcmTokenManager.clearToken() }
                            auth.signOut()
                            navigateToInitial()
                        }
                    }
                ) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { PeruToursBrand(compact = true) },
                actions = {
                    TextButton(onClick = navigateToProfile) { Text("Mi perfil") }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        bottomBar = {
            CustomerNavigationBar(
                selected = CustomerSection.HOME,
                onHome = {},
                onQuotations = ::openQuotations,
                onReservations = navigateToReservations,
                onTrips = navigateToTrips,
                onChat = navigateToChat
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "greeting") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hola, " + uiState.userName.substringBefore(" ").ifBlank { "viajero" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("¿A dónde vamos?", style = MaterialTheme.typography.headlineLarge)
                    }
                }
                item(key = "custom-trip") {
                    Box(Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.extraLarge)) {
                        Image(painterResource(R.drawable.welcome_colca), "Cañón del Colca, Arequipa",
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                            0.15f to Color.Transparent, 1f to Color(0xE610251B))))
                        Column(Modifier.align(Alignment.BottomStart).padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("UN VIAJE A TU MEDIDA", style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f))
                            Text("Empieza por el lugar.\nNosotros armamos la ruta.",
                                style = MaterialTheme.typography.headlineSmall, color = Color.White)
                            Button(onClick = { navigateToTravelRequest(null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary)) {
                                Text("Diseñar mi viaje")
                            }
                        }
                    }
                }
                item(key = "recommendation-heading") {
                    SectionHeading("Pensados para ti", supportingText = "Ideas según tus preferencias",
                        action = {
                            IconButton(onClick = { recommendationViewModel.refresh() },
                                enabled = recommendationState !is RecommendationUiState.Loading) {
                                Icon(Icons.Default.Refresh, "Actualizar recomendaciones")
                            }
                        })
                }
                item(key = "recommendations") {
                    when (val state = recommendationState) {
                        is RecommendationUiState.Success -> LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(state.recommendations, key = { it.destinationId }) { recommendation ->
                                RecommendationCard(recommendation) { navigateToDetail(recommendation.destinationId) }
                            }
                        }
                        is RecommendationUiState.Loading -> DestinationLoadingState()
                        is RecommendationUiState.Error -> TravelEmptyState(
                            "No pudimos preparar tus recomendaciones", state.message,
                            Icons.Default.AutoAwesome, actionLabel = "Reintentar",
                            onAction = { recommendationViewModel.loadRecommendations() })
                        is RecommendationUiState.Empty -> TravelEmptyState(
                            "Hagamos el viaje más tuyo",
                            "Añade tus preferencias para recibir ideas que encajen contigo.",
                            Icons.Default.AutoAwesome, actionLabel = "Elegir preferencias",
                            onAction = navigateToProfile)
                    }
                }
                item(key = "catalog-heading") {
                    SectionHeading("Encuentra tu próximo viaje",
                        supportingText = "Paquetes de nuestra agencia",
                        action = {
                            IconButton(onClick = { viewModel.fetchDestinations() }) {
                                Icon(Icons.Default.Refresh, "Actualizar catálogo")
                            }
                        })
                }
                item(key = "catalog-search") {
                    OutlinedTextField(
                        value = uiState.searchQuery, onValueChange = viewModel::searchCatalog,
                        label = { Text("Buscar un viaje") },
                        placeholder = { Text("Destino, paquete o actividad") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
                if (uiState.categories.size > 1) {
                    item(key = "catalog-categories") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.categories, key = { it }) { category ->
                                FilterChip(selected = uiState.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    label = { Text(category) })
                            }
                        }
                    }
                }
                when (val state = uiState.destinationsState) {
                    HomeDestinationsState.Loading -> item { DestinationLoadingState() }
                    is HomeDestinationsState.Error -> item {
                        DestinationErrorState(state.message) { viewModel.fetchDestinations() }
                    }
                    is HomeDestinationsState.Success -> {
                        if (state.fromCache) item {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CloudOff, null, Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Catálogo guardado. Los precios se confirman al cotizar.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (uiState.visiblePackages.isEmpty()) item {
                            TravelEmptyState("No encontramos ese viaje",
                                "Prueba con otra ciudad o cambia los filtros.",
                                Icons.Outlined.Search, actionLabel = "Limpiar filtros",
                                onAction = viewModel::clearCatalogFilters)
                        }
                        items(uiState.visiblePackages, key = { it.id }) { travel ->
                            TravelPackageCard(
                                travel = travel,
                                onDetails = { navigateToDetail(travel.destinationId) },
                                onRequest = { navigateToTravelRequest(travel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DestinationLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Buscando destinos...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DestinationErrorState(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No pudimos cargar los destinos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            TextButton(onClick = onRetry) { Text("Intentar de nuevo") }
        }
    }
}

/**
 * HU10: tarjeta de la sección "Recomendado para ti".
 * Muestra el motivo personalizado, la procedencia (IA o reglas locales)
 * y el resumen turístico externo cuando está disponible.
 */
@Composable
private fun RecommendationCard(
    recommendation: Recommendation,
    onClick: () -> Unit
) {
    val destination = recommendation.destination

    Card(
        modifier = Modifier
            .width(264.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                TravelPhoto(destination?.imageUrl.orEmpty(), recommendation.title,
                    Modifier.fillMaxSize())
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Recomendaciones",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = destination?.rating.orEmpty().ifBlank { "—" },
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    destination?.price?.let { price ->
                        Text(
                            text = price,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Información turística externa (Wikipedia), si estuvo disponible.
                if (recommendation.summary.isNotBlank()) {
                    Text(
                        text = recommendation.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
