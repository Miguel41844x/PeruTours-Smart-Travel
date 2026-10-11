package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.perutours.ui.components.ScreenHorizontalPadding
import com.example.perutours.ui.components.SectionHeading
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentHomeScreen(
    auth: FirebaseAuth,
    onViewRequests: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val currentUser = auth.currentUser
    val uid = currentUser?.uid.orEmpty()
    val rawName = currentUser?.displayName.orEmpty()
    var agentName by remember {
        mutableStateOf(
            rawName.substringBefore("|").trim().takeIf(String::isNotBlank)
                ?: currentUser?.email?.substringBefore("@")
                ?: "Agente"
        )
    }
    var photoUrl by remember { mutableStateOf(currentUser?.photoUrl?.toString().orEmpty()) }
    var pendingCount by remember { mutableIntStateOf(0) }
    var quotedCount by remember { mutableIntStateOf(0) }
    var isLoadingCounts by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val firestore = FirebaseFirestore.getInstance()
        val userListener = uid.takeIf(String::isNotEmpty)?.let { userId ->
            firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot?.exists() == true) {
                        snapshot.getString("name")?.takeIf(String::isNotBlank)?.let { agentName = it }
                        snapshot.getString("photoUrl")?.let { photoUrl = it }
                    }
                }
        }
        val requestsListener = firestore.collection("travel_requests")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    pendingCount = snapshot.documents.count { document ->
                        document.getString("status").orEmpty().contains("Pendiente", ignoreCase = true)
                    }
                    quotedCount = snapshot.documents.count { document ->
                        document.getString("status").orEmpty().equals("Cotizado", ignoreCase = true)
                    }
                    isLoadingCounts = false
                }
            }
        onDispose {
            userListener?.remove()
            requestsListener.remove()
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Tendrás que ingresar nuevamente para gestionar las solicitudes.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AgentAvatar(name = agentName, photoUrl = photoUrl)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = agentName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Agente de viajes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenHorizontalPadding, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 900.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Resumen de hoy",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Prioriza las solicitudes pendientes y prepara las propuestas de los clientes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        label = "Pendientes",
                        value = if (isLoadingCounts) "—" else pendingCount.toString(),
                        icon = { Icon(Icons.Default.HourglassEmpty, contentDescription = null) },
                        emphasized = true,
                        onClick = onViewRequests,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Cotizadas",
                        value = if (isLoadingCounts) "—" else quotedCount.toString(),
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        emphasized = false,
                        onClick = onViewRequests,
                        modifier = Modifier.weight(1f)
                    )
                }

                SectionHeading(
                    title = "Solicitudes de viaje",
                    supportingText = if (pendingCount > 0) {
                        "$pendingCount ${if (pendingCount == 1) "solicitud necesita" else "solicitudes necesitan"} una cotización."
                    } else {
                        "No hay solicitudes pendientes por ahora."
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    Icons.Default.RequestQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(12.dp).size(24.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bandeja de solicitudes", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Revisa rutas, fechas y datos del viajero antes de cotizar.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onViewRequests,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Abrir solicitudes")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentAvatar(name: String, photoUrl: String) {
    if (photoUrl.isNotEmpty()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Foto del agente",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: @Composable () -> Unit,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (emphasized) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.tertiaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) { icon() }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
