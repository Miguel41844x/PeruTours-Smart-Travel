package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perutours.data.model.TravelRequest
import com.example.perutours.ui.theme.BackgroundLight
import com.example.perutours.ui.theme.PeruGold40
import com.example.perutours.ui.theme.SurfaceLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentRequestsScreen(
    onNavigateBack: () -> Unit,
    onRequestSelected: (String) -> Unit,
    viewModel: AgentRequestsViewModel = viewModel()
) {
    val requests by viewModel.requests.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Filtro por defecto: "Pendientes" para que el agente vea primero lo que falta cotizar
    var selectedFilter by remember { mutableStateOf("Pendientes") }

    val pendingCount = remember(requests) {
        requests.count { it.status.contains("Pendiente", ignoreCase = true) }
    }
    val quotedCount = remember(requests) {
        requests.count { it.status.equals("Cotizado", ignoreCase = true) }
    }

    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "Pendientes" -> requests.filter { it.status.contains("Pendiente", ignoreCase = true) }
            "Cotizados" -> requests.filter { it.status.equals("Cotizado", ignoreCase = true) }
            else -> requests
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bandeja de Solicitudes",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredRequests.size} solicitudes ($pendingCount pendientes • $quotedCount cotizadas)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRequests() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar",
                            tint = PeruGold40
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Chips de Filtro con Contadores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chip Pendientes
                FilterChip(
                    selected = selectedFilter == "Pendientes",
                    onClick = { selectedFilter = "Pendientes" },
                    label = { Text("Pendientes ($pendingCount)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFEF3C7),
                        selectedLabelColor = Color(0xFFB45309),
                        selectedLeadingIconColor = Color(0xFFB45309)
                    )
                )

                // Chip Cotizados
                FilterChip(
                    selected = selectedFilter == "Cotizados",
                    onClick = { selectedFilter = "Cotizados" },
                    label = { Text("Cotizados ($quotedCount)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD1FAE5),
                        selectedLabelColor = Color(0xFF065F46),
                        selectedLeadingIconColor = Color(0xFF065F46)
                    )
                )

                // Chip Todos
                FilterChip(
                    selected = selectedFilter == "Todos",
                    onClick = { selectedFilter = "Todos" },
                    label = { Text("Todos (${requests.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PeruGold40,
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Manejo de Estados
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PeruGold40)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Consultando solicitudes en Firestore...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error al consultar solicitudes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error ?: "",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = { viewModel.loadRequests() },
                            colors = ButtonDefaults.buttonColors(containerColor = PeruGold40)
                        ) {
                            Text("Reintentar conexión")
                        }
                    }
                }

                filteredRequests.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                modifier = Modifier.size(70.dp),
                                shape = CircleShape,
                                color = Color(0xFFFEF3C7)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = PeruGold40,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay solicitudes en '$selectedFilter'",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (selectedFilter == "Pendientes") {
                                    "¡Excelente! No tienes solicitudes pendientes por cotizar."
                                } else {
                                    "Las solicitudes aparecerán aquí a medida que se procesen."
                                },
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(
                            items = filteredRequests,
                            key = { it.id }
                        ) { request ->
                            TravelRequestCard(
                                request = request,
                                onClick = { onRequestSelected(request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TravelRequestCard(
    request: TravelRequest,
    onClick: () -> Unit
) {
    val isPending = request.status.contains("Pendiente", ignoreCase = true)
    val isQuoted = request.status.equals("Cotizado", ignoreCase = true)

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "PE")) }
    val departureStr = if (request.departureAtMillis > 0L) {
        dateFormat.format(Date(request.departureAtMillis))
    } else "Por definir"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = BorderStroke(
            width = if (isPending) 1.5.dp else 1.dp,
            color = if (isPending) Color(0xFFFDE68A) else Color(0xFFE5E7EB)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            // Header con Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ID: #${request.id.takeLast(6).uppercase()}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9CA3AF)
                )

                Surface(
                    color = when {
                        isQuoted -> Color(0xFFD1FAE5)
                        isPending -> Color(0xFFFEF3C7)
                        else -> Color(0xFFE5E7EB)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = request.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isQuoted -> Color(0xFF065F46)
                            isPending -> Color(0xFFB45309)
                            else -> Color(0xFF374151)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Destino
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = PeruGold40,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.destination,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Origen y Pasajeros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlightTakeoff,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Desde: ${request.originCity.ifBlank { "No especificado" }}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${request.travelerCount} viajero(s)",
                            fontSize = 11.5.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Chip Fecha tentativa
            Surface(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF4B5563),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Fecha tentativa: $departureStr",
                        fontSize = 11.5.sp,
                        color = Color(0xFF374151)
                    )
                }
            }

            // Notas del cliente
            if (request.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(10.dp)
                ) {
                    Row {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "\"${request.notes}\"",
                            fontSize = 11.5.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botón de Acción
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPending) Color(0xFFD97706) else Color(0xFF059669)
                )
            ) {
                Text(
                    text = if (isPending) "Elaborar Cotización (HU05)" else "Ver Cotización Emitida ('Cotizado')",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}