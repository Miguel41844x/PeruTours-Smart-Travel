package com.example.perutours.ui.screens.home

import com.example.perutours.data.remote.DestinationDto

// Criterio 2: Manejo reactivo de estados con sealed class
sealed class HomeDestinationsState {
    object Loading : HomeDestinationsState()
    data class Success(val destinations: List<DestinationDto>, val fromCache: Boolean = false) : HomeDestinationsState()
    data class Error(val message: String) : HomeDestinationsState()
}

data class HomeUiState(
    val userName: String = "",
    val photoUrl: String = "",
    val preferenceNames: List<String> = emptyList(),
    val destinationsState: HomeDestinationsState = HomeDestinationsState.Loading,
    val selectedCategory: String = "Todos",
    val searchQuery: String = ""
) {
    val catalog: List<DestinationDto>
        get() = (destinationsState as? HomeDestinationsState.Success)?.destinations.orEmpty()

    val categories: List<String>
        get() = listOf("Todos") + catalog.map { it.category }.filter(String::isNotBlank).distinct().sorted()

    val visiblePackages: List<DestinationDto>
        get() {
            val query = normalizeCatalogQuery(searchQuery.trim())
            return catalog.filter {
                (selectedCategory == "Todos" || it.category == selectedCategory) &&
                    (query.isBlank() || normalizeCatalogQuery(
                        listOf(it.title, it.location, it.category, it.description.orEmpty()).joinToString(" ")
                    ).contains(query))
            }
        }
}

private fun normalizeCatalogQuery(value: String): String =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT)
