package com.example.perutours.data.repository

import com.example.perutours.data.local.DestinationDao
import com.example.perutours.data.local.DestinationEntity
import com.example.perutours.data.local.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Catálogo que ve el cliente.
 *
 * Antes consumía un JSON alojado en GitHub; ahora consulta los paquetes
 * PUBLICADOS de Firestore y los guarda en Room. Room sigue siendo la fuente
 * única de origen de la UI y conserva la caché con TTL para modo offline.
 */
class DestinationRepository(
    private val packageRepository: PackageRepository,
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

    suspend fun refreshDestinations(forceRefresh: Boolean = false): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                // Si el caché local aún es válido y no se forzó el refresco,
                // usamos Room sin tocar la red.
                if (!forceRefresh && !isCacheExpired()) {
                    val currentData = destinationDao.getAllDestinationsSync()
                    if (currentData.isNotEmpty()) {
                        return@withContext Result.success(Unit)
                    }
                }

                // 1. Solo los paquetes PUBLICADOS llegan al cliente; las
                //    reglas de Firestore lo garantizan además del filtro.
                val publishedPackages = packageRepository.getPublishedPackages()

                // 2. Se resuelven los destinos referenciados para lugar y
                //    coordenadas (puntos de interés cercanos).
                val destinationsById = packageRepository.getDestinationsByIds(
                    publishedPackages.map { it.destinationId }
                )

                // 3. Reemplazo atómico transaccional en Room.
                val entities = publishedPackages.map { tourPackage ->
                    tourPackage.toEntity(destinationsById[tourPackage.destinationId])
                }
                destinationDao.syncRemoteDestinations(entities)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
