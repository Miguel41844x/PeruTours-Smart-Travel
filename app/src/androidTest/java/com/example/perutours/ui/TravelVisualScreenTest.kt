package com.example.perutours.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.example.perutours.data.remote.DestinationDto
import com.example.perutours.ui.components.TravelPackageCard
import com.example.perutours.ui.screens.initial.InitialScreen
import com.example.perutours.ui.theme.PeruToursTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class TravelVisualScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomeActionsRemainReachableWithLargeText() {
        var selected = false
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.6f)) {
                PeruToursTheme {
                    InitialScreen(navigateToLogin = {}, navigateToSignUp = { selected = true })
                }
            }
        }
        compose.onNodeWithText("Empezar mi viaje").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(selected)
    }

    @Test fun packageHasAccessibleActionsAndADarkThemeImageFallback() {
        var requested = false
        compose.setContent {
            PeruToursTheme(darkTheme = true) {
                Column(Modifier.padding(20.dp)) {
                    TravelPackageCard(
                        travel = DestinationDto("test", "arequipa", "Cañón del Colca", "Arequipa",
                            "2 días", "S/ 250", "", "Naturaleza", ""),
                        onDetails = {}, onRequest = { requested = true }
                    )
                }
            }
        }
        compose.onNodeWithText("Solicitar cotización").assertIsDisplayed().performClick()
        assertTrue(requested)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "visual-package-dark.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
