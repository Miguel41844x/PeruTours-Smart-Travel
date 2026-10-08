package com.example.perutours.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.perutours.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RoleRouterScreen(
    navController: NavHostController,
    auth: FirebaseAuth
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

        try {

            val repository = ProfileRepository()

            val profile =
                repository.getProfile(currentUser.uid)

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

        } catch (e: Exception) {

            errorMessage =
                e.localizedMessage
                    ?: "No se pudo obtener el rol del usuario."

        } finally {

            isLoading = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        if (isLoading) {

            CircularProgressIndicator()

        } else if (errorMessage != null) {

            Text(
                text = errorMessage!!
            )
        }
    }
}