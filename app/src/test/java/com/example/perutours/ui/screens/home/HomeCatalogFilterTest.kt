package com.example.perutours.ui.screens.home

import com.example.perutours.data.remote.DestinationDto
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeCatalogFilterTest {
    private val state = HomeUiState(destinationsState = HomeDestinationsState.Success(listOf(
        travel("colca", "Ruta del Colca", "Arequipa", "Naturaleza"),
        travel("cusco", "Cusco histórico", "Cusco", "Cultura")
    )))

    @Test fun filtersByAccentedLocationAndCategoryTogether() {
        assertEquals(listOf("cusco"), state.copy(searchQuery = "HISTORICO").visiblePackages.map { it.id })
        assertEquals(emptyList<String>(), state.copy(searchQuery = "Cusco",
            selectedCategory = "Naturaleza").visiblePackages.map { it.id })
    }

    @Test fun clearingFiltersShowsTheOriginalCatalogAndEmptyQueriesAreSafe() {
        assertEquals(2, state.copy(searchQuery = "  ").visiblePackages.size)
        assertEquals(listOf("Todos", "Cultura", "Naturaleza"), state.categories)
        assertEquals(emptyList<DestinationDto>(), HomeUiState().visiblePackages)
    }

    private fun travel(id: String, name: String, location: String, category: String) =
        DestinationDto(id = id, destinationId = id, title = name, location = location,
            duration = "2 días", price = "S/ 100", rating = "", category = category, imageUrl = "")
}
