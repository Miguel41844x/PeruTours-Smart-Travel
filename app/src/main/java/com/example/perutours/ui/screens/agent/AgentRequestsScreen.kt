package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perutours.data.model.TravelRequest

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Solicitudes de viaje")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadRequests()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualizar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        when {
            isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Ocurrió un error",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = error ?: ""
                    )

                    Button(
                        onClick = {
                            viewModel.loadRequests()
                        }
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            requests.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    verticalArrangement =
                        Arrangement.Center
                ) {
                    Text(
                        text = "No hay solicitudes pendientes",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "Las nuevas solicitudes de los clientes aparecerán aquí."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding =
                        PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    items(
                        items = requests,
                        key = { it.id }
                    ) { request ->

                        TravelRequestCard(
                            request = request,
                            onClick = {
                                onRequestSelected(request.id)
                            }
                        )
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
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = request.destination,
                style =
                    MaterialTheme.typography.titleLarge
            )

            Text(
                text =
                    "Origen: ${request.originCity}"
            )

            Text(
                text =
                    "Viajeros: ${request.travelerCount}"
            )

            Text(
                text =
                    "Estado: ${request.status}"
            )

            if (request.notes.isNotBlank()) {
                Text(
                    text =
                        "Notas: ${request.notes}"
                )
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Preparar cotización")
            }
        }
    }
}