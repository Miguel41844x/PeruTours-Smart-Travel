package com.perutours.smarttravel.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.perutours.smarttravel.presentation.auth.AuthUiState
import com.perutours.smarttravel.presentation.auth.AuthViewModel
import com.perutours.smarttravel.presentation.auth.LoginScreen
import com.perutours.smarttravel.presentation.auth.RegisterScreen
import com.perutours.smarttravel.presentation.auth.RegisterViewModel
import com.perutours.smarttravel.presentation.auth.VerifyEmailScreen

@Composable
fun AuthNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    registerViewModel: RegisterViewModel = viewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()

    val startDestination = when (uiState) {
        is AuthUiState.Authenticated -> Screen.Home.route
        is AuthUiState.EmailNotVerified -> {
            val user = (uiState as AuthUiState.EmailNotVerified).user
            Screen.VerifyEmail.createRoute(user.email)
        }
        else -> Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = { email ->
                    navController.navigate(Screen.VerifyEmail.createRoute(email)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = {
                    val formEmail = registerViewModel.formState.value.email.value
                    navController.navigate(Screen.VerifyEmail.createRoute(formEmail)) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.VerifyEmail.route,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            VerifyEmailScreen(
                viewModel = authViewModel,
                email = email,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomePlaceholder(
                userName = (uiState as? AuthUiState.Authenticated)?.user?.displayName ?: "Turista",
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
