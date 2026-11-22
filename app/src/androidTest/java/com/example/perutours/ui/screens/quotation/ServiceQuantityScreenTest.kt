package com.example.perutours.ui.screens.quotation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.perutours.data.model.CatalogService
import com.example.perutours.ui.screens.quotation.components.ServiceCatalogItemCard
import org.junit.Rule
import org.junit.Test

class ServiceQuantityScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun changesQuantityAndSubtotalWithoutRemovingTheService() {
        var quantity by mutableStateOf(2)
        var selected by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                ServiceCatalogItemCard(
                    catalogService = CatalogService(name = "Hotel de demostración", unitCost = 55.0),
                    isSelected = selected,
                    onToggle = { selected = !selected },
                    quantity = quantity,
                    onQuantityDelta = { quantity = (quantity + it).coerceIn(1, 100) }
                )
            }
        }
        compose.onNodeWithContentDescription("Aumentar cantidad de Hotel de demostración").performClick()
        compose.onNodeWithText("Subtotal: S/ 165.00", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Reducir cantidad de Hotel de demostración").performClick()
        compose.onNodeWithText("Subtotal: S/ 110.00", useUnmergedTree = true).assertIsDisplayed()
    }
}
