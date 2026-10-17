package com.example.perutours.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.perutours.data.messaging.FCMTokenManager
import com.example.perutours.data.repository.ProfileRepository
import com.example.perutours.ui.components.PeruToursBrand
import com.google.firebase.auth.FirebaseAuth
import org.koin.compose.koinInject

@Composable
fun RoleRouterScreen(
    navController: NavHostController,
    auth: FirebaseAuth,
    profileRepository: ProfileRepository = koinInject(),
    fcmTokenManager: FCMTokenManager = koinInject()
) {

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        val currentUser = auth.currentUser

        if (currentUser == null) {

            navController.navigate("initial") {
                popUpTo(0) {
                    inclusive = true
                }
            }

            return@LaunchedEffect
        }

        // Registrar/actualizar token FCM al inicio de sesión o entrada persistente
        runCatching { fcmTokenManager.saveToken() }

        try {

            val profile = profileRepository.getProfile(currentUser.uid)

            when (profile.role.lowercase()) {

                "agente" -> {

                    navController.navigate("agent_home") {

                        popUpTo("role_router") {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }

                else -> {

                    navController.navigate("home") {

                        popUpTo("role_router") {
                            inclusive = true
                        }

                        launchSingleTop = true
                    }
                }
            }

        } catch (_: Exception) {

            errorMessage =
                "No pudimos abrir tu cuenta. Revisa tu conexión e inténtalo nuevamente."

        } finally {

            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                PeruToursBrand()
                Spacer(Modifier.height(32.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Estamos preparando tu espacio de viaje...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}
