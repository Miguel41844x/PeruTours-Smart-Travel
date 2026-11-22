package com.example.perutours.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perutours.data.model.FavoriteItem
import com.example.perutours.data.model.FavoriteType
import com.example.perutours.data.repository.FavoriteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val loading: Boolean = true,
    val items: List<FavoriteItem> = emptyList(),
    val busyKeys: Set<String> = emptySet(),
    val message: String? = null,
    val query: String = "",
    val type: FavoriteType? = null
) {
    fun contains(id: String, type: FavoriteType) = items.any { it.id == id && it.type == type }
    val visibleItems: List<FavoriteItem> get() = items.filter {
        (type == null || it.type == type) &&
            (query.isBlank() || (it.title + " " + it.location).contains(query.trim(), ignoreCase = true))
    }
}

class FavoritesViewModel(private val repository: FavoriteDataSource) : ViewModel() {
    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites().catch {
                _uiState.update { it.copy(loading = false, message = "No pudimos leer tus favoritos.") }
            }.collect { items ->
                _uiState.update { it.copy(loading = false, items = items) }
            }
        }
    }

    fun search(value: String) { _uiState.update { it.copy(query = value.take(120)) } }
    fun filter(type: FavoriteType?) { _uiState.update { it.copy(type = type) } }
    fun clearMessage() { _uiState.update { it.copy(message = null) } }

    fun toggle(item: FavoriteItem) {
        if (item.key in _uiState.value.busyKeys) return
        _uiState.update { it.copy(busyKeys = it.busyKeys + item.key) }
        viewModelScope.launch {
            try {
                val saved = repository.toggle(item)
                _uiState.update { it.copy(message = if (saved) "Guardado en favoritos." else "Quitado de favoritos.") }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(message = "No pudimos cambiar este favorito. Inténtalo nuevamente.") }
            } finally {
                _uiState.update { it.copy(busyKeys = it.busyKeys - item.key) }
            }
        }
    }
}
