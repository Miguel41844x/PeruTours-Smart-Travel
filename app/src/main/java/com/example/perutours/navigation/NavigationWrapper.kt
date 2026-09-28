package com.example.perutours.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.perutours.ui.screens.home.HomeScreen
import com.example.perutours.ui.screens.initial.InitialScreen
import com.example.perutours.ui.screens.login.LoginScreen
import com.example.perutours.ui.screens.signup.SignUpScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun NavigationWrapper(
    navHostController: NavHostController,
    auth: FirebaseAuth
) {
    // Si ya hay usuario conectado, entra directo al home
    val startDestination = if (auth.currentUser != null) "home" else "initial"

    NavHost(navController = navHostController, startDestination = startDestination) {

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
                navigateToInitial = {
                    navHostController.navigate(route = "initial") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}