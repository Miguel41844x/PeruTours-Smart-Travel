package com.example.perutours.data.repository

import com.example.perutours.data.local.FavoriteDao
import com.example.perutours.data.local.toEntity
import com.example.perutours.data.model.FavoriteItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

interface FavoriteDataSource {
    fun observeFavorites(): Flow<List<FavoriteItem>>
    suspend fun toggle(item: FavoriteItem): Boolean
}

class FavoriteRepository(private val dao: FavoriteDao, private val auth: FirebaseAuth) : FavoriteDataSource {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFavorites(): Flow<List<FavoriteItem>> = callbackFlow<String?> {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged().flatMapLatest { uid ->
        if (uid.isNullOrBlank()) flowOf(emptyList()) else dao.observeAll(uid).map { rows ->
            rows.mapNotNull { runCatching { it.toModel() }.getOrNull() }
        }
    }

    override suspend fun toggle(item: FavoriteItem): Boolean {
        val uid = auth.currentUser?.uid?.takeIf(String::isNotBlank)
            ?: error("Inicia sesión para guardar favoritos.")
        require(item.id.isNotBlank() && item.destinationId.isNotBlank() &&
            item.title.isNotBlank() && item.title.length <= 200) {
            "No se puede guardar este elemento del catálogo."
        }
        return dao.toggle(item.toEntity(uid))
    }
}
