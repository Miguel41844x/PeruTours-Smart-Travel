package com.example.perutours.ui.screens.agent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentHomeScreen(
    auth: FirebaseAuth,
    onViewRequests: () -> Unit,
    onLogout: () -> Unit
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Panel del agente")
                },

                actions = {

                    IconButton(
                        onClick = onLogout
                    ) {

                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(20.dp)
        ) {

            Text(
                text = "Bienvenido, agente",
                style =
                    MaterialTheme.typography.headlineSmall
            )

            Text(
                text = auth.currentUser?.email
                    ?: "Agente PeruTours",
                style =
                    MaterialTheme.typography.bodyLarge
            )

            Card(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Solicitudes de viaje",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text =
                            "Consulta las solicitudes pendientes y prepara sus cotizaciones."
                    )

                    Button(
                        onClick = onViewRequests,
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.RequestQuote,
                            contentDescription = null
                        )

                        Text(
                            text = "Ver solicitudes"
                        )
                    }
                }
            }
        }
    }
}