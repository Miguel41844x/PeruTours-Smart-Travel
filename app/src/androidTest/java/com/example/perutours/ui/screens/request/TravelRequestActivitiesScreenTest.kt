package com.example.perutours.ui.screens.request

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.perutours.data.model.CatalogService
import com.example.perutours.data.model.TourDestination
import org.junit.Rule
import org.junit.Test

class TravelRequestActivitiesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun activityLabelTogglesTheInterestAndSavingPreventsChanges() {
        var state by mutableStateOf(
            TravelRequestUiState(
                catalogDestination = TourDestination(id = "arequipa"),
                availableActivities = listOf(CatalogService(id = "colca", name = "Cañón del Colca"))
            )
        )
        compose.setContent {
            MaterialTheme {
                Column {
                    RequestedActivities(
                        state = state,
                        onToggle = { id -> state = state.copy(requestedServiceIds = listOf(id)) },
                        onRetry = {}
                    )
                }
            }
        }
        compose.onNodeWithText("Cañón del Colca").assertIsOff().performClick().assertIsOn()
        compose.runOnIdle { state = state.copy(isSaving = true) }
        compose.onNodeWithText("Cañón del Colca").assertIsNotEnabled()
    }
}
