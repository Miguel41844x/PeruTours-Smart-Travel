package com.example.perutours.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.perutours.data.model.FavoriteItem
import com.example.perutours.data.model.FavoriteType
import com.example.perutours.domain.payment.*
import com.example.perutours.ui.components.VoucherOcrPanel
import com.example.perutours.ui.screens.favorites.FavoriteCard
import com.example.perutours.ui.screens.payment.VoucherOcrState
import com.example.perutours.ui.theme.PeruToursTheme
import java.math.BigDecimal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FavoritesAndVoucherScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun futureVoucherShowsTheProblemInsteadOfClaimingItIsValid() {
        compose.setContent {
            PeruToursTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VoucherOcrPanel(
                        state = VoucherOcrState.Read(VoucherOcrResult(
                            VoucherAmount(BigDecimal("120"), "PEN"), "00123456",
                            VoucherDate(2050, 1, 1), VoucherTime(10, 0))),
                        expectedAmount = 120.0, expectedCurrency = "PEN")
                }
            }
        }
        compose.onNodeWithText("Hay datos que debes corregir").assertIsDisplayed()
        compose.onNodeWithText("La fecha u hora del comprobante está en el futuro.").assertIsDisplayed()
    }

    @Test fun savedDescriptionCanBeReadAndRemovedWithoutLoadingTheRemoteCatalog() {
        var removed = false
        compose.setContent {
            PeruToursTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    FavoriteCard(FavoriteItem("colca", FavoriteType.DESTINATION, "colca",
                        "Cañón del Colca", "Arequipa", "Descripción disponible sin internet.", ""),
                        busy = false, onRemove = { removed = true }, onOpen = {})
                }
            }
        }
        compose.onNodeWithText("Descripción disponible sin internet.").assertIsDisplayed()
        compose.onNodeWithText("Leer descripción completa").performClick()
        compose.onNodeWithText("Ver menos").assertIsDisplayed()
        compose.onNodeWithContentDescription("Quitar de favoritos: Cañón del Colca").performClick()
        assertTrue(removed)
    }
}
