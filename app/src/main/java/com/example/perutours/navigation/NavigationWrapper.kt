package com.example.perutours.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.perutours.ui.screens.home.HomeScreen
import com.example.perutours.ui.screens.initial.InitialScreen
import com.example.perutours.ui.screens.login.LoginScreen
import com.example.perutours.ui.screens.profile.ProfileScreen
import com.example.perutours.ui.screens.request.TravelRequestScreen
import com.example.perutours.ui.screens.signup.SignUpScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth
) {
    val currentUser = auth.currentUser

    // CRITERIO 3: Persistencia de sesión
    // Solo si el usuario ya inició sesión Y su correo ya está verificado, entra directo a Home
    val startDestination = if (currentUser != null && currentUser.isEmailVerified) {
        "home"
    } else {
        "initial"
    }

    NavHost(
        navController = navHostController,
        startDestination = startDestination
    ) {

        composable(route = "initial") {
            InitialScreen(
                navigateToLogin = { navHostController.navigate(route = "logIn") },
                navigateToSignUp = { navHostController.navigate(route = "signUp") }
            )
        }

        composable(route = "logIn") {
            LoginScreen(
                auth = auth,
                navigateToSignUp = { navHostController.navigate(route = "signUp") },
                navigateToHome = {
                    navHostController.navigate(route = "home") {
                        popUpTo("initial") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "signUp") {
            SignUpScreen(
                auth = auth,
                navigateToLogin = {
                    navHostController.navigate(route = "logIn") {
                        popUpTo("signUp") { inclusive = true }
                    }
                }
            )
        }

        composable(route = "home") {
            HomeScreen(
                auth = auth,
                navigateToTravelRequest = {
                    navHostController.navigate(route = "travelRequest")
                },
                navigateToProfile = {
                    navHostController.navigate(route = "profile")
                },
                navigateToInitial = {
                    navHostController.navigate(route = "initial") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Gestión de Perfil y Preferencias
        composable(route = "profile") {
            ProfileScreen(
                onBack = {
                    navHostController.popBackStack()
                }
            )
        }

        composable(route = "travelRequest") {
            TravelRequestScreen(
                onBack = { navHostController.popBackStack() },
                onSaved = {
                    navHostController.navigate(route = "home") {
                        popUpTo("travelRequest") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
