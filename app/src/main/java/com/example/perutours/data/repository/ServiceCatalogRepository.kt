package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogService
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Catálogo dinámico de servicios vendibles.
 *
 * Cada servicio referencia por ID al destino y al proveedor. Los nombres de
 * ciudad/proveedor se guardan como snapshot de presentación para que las
 * cotizaciones históricas no cambien cuando se edite el catálogo.
 */
class ServiceCatalogRepository(
    private val firestore: FirebaseFirestore
) {

    suspend fun getServices(includeInactive: Boolean = false): List<CatalogService> {
        val snapshot = firestore.collection(COLLECTION_SERVICES).get().await()
        return snapshot.toObjects(CatalogService::class.java)
            .filter { includeInactive || it.active }
            .sortedWith(
                compareBy(
                    CatalogService::city,
                    CatalogService::category,
                    CatalogService::name
                )
            )
    }

    suspend fun getService(serviceId: String): CatalogService? {
        if (serviceId.isBlank()) return null
        return firestore.collection(COLLECTION_SERVICES)
            .document(serviceId)
            .get()
            .await()
            .toObject(CatalogService::class.java)
    }

    suspend fun getRequestableActivities(destinationId: String): List<CatalogService> {
        if (destinationId.isBlank()) return emptyList()
        return firestore.collection(COLLECTION_SERVICES)
            .whereEqualTo("destinationId", destinationId)
            .whereEqualTo("active", true)
            .whereEqualTo("requestable", true)
            .get().await()
            .toObjects(CatalogService::class.java)
            .sortedBy(CatalogService::name)
    }

    suspend fun getServicesByIds(ids: List<String>): Map<String, CatalogService> =
        ids.filter { it.isNotBlank() }.distinct().chunked(10).flatMap { chunk ->
            firestore.collection(COLLECTION_SERVICES)
                .whereIn(FieldPath.documentId(), chunk)
                .get().await()
                .documents.mapNotNull { document ->
                    document.toObject(CatalogService::class.java)?.copy(id = document.id)
                }
        }.associateBy(CatalogService::id)

    suspend fun saveService(service: CatalogService): CatalogService {
        val id = service.id.ifBlank {
            firestore.collection(COLLECTION_SERVICES).document().id
        }
        val persisted = service.copy(
            id = id,
            name = service.name.trim(),
            city = service.city.trim(),
            category = service.category.trim(),
            supplierName = service.supplierName.trim(),
            description = service.description.trim(),
            defaultQuantity = service.defaultQuantity.coerceIn(1, 100),
            durationMinutes = service.durationMinutes.coerceIn(15, 10_080)
        )
        firestore.collection(COLLECTION_SERVICES)
            .document(id)
            .set(persisted)
            .await()
        return persisted
    }

    suspend fun setActive(serviceId: String, active: Boolean) {
        require(serviceId.isNotBlank()) { "No se recibió el servicio." }
        firestore.collection(COLLECTION_SERVICES)
            .document(serviceId)
            .update("active", active)
            .await()
    }

    companion object {
        const val COLLECTION_SERVICES = "catalog_services"
    }
}
