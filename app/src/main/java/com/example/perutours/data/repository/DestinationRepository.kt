package com.example.perutours.data.repository

import com.example.perutours.data.local.DestinationDao
import com.example.perutours.data.local.DestinationEntity
import com.example.perutours.data.local.toEntity
import com.example.perutours.data.remote.DestinationsApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DestinationRepository(
    private val apiService: DestinationsApiService,
    private val destinationDao: DestinationDao,
    private val cacheTtlMillis: Long = 60 * 60 * 1000L // 1 hora de validez
) {
    // Escucha la base de datos local Room (Single Source of Truth)
    val localDestinations: Flow<List<DestinationEntity>> = destinationDao.getAllDestinations()

    suspend fun isCacheExpired(): Boolean = withContext(Dispatchers.IO) {
        val lastUpdate = destinationDao.getLastUpdatedTimestamp() ?: return@withContext true
        val timePassed = System.currentTimeMillis() - lastUpdate
        timePassed > cacheTtlMillis
    }

    suspend fun refreshDestinations(forceRefresh: Boolean = false): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Si el caché local aún es válido y no se forzó el refresco, usamos la caché
            if (!forceRefresh && !isCacheExpired()) {
                val currentData = destinationDao.getAllDestinationsSync()
                if (currentData.isNotEmpty()) {
                    return@withContext Result.success(Unit)
                }
            }

            // Descarga de la API remota
            val remoteDestinations = apiService.getDestinations()
            val now = System.currentTimeMillis()
            val entities = remoteDestinations.map { it.toEntity(now) }

            // Reemplazo atómico transaccional para eliminar registros obsoletos
            destinationDao.syncRemoteDestinations(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}