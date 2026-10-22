package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import com.example.perutours.data.model.TourPackage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CRUD de paquetes sobre Firestore (colección `packages`).
 *
 * Un paquete es la oferta vendible de un destino: precio, duración, imagen,
 * condiciones y servicios incluidos. Su estado gobierna la visibilidad para
 * el cliente (solo PUBLICADO aparece en el catálogo).
 */
class PackageRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun getAllPackages(): List<TourPackage> = firestore
        .collection(COLLECTION_PACKAGES)
        .orderBy(FIELD_UPDATED_AT)
        .get()
        .await()
        .toObjects(TourPackage::class.java)

    suspend fun getPackagesByDestination(destinationId: String): List<TourPackage> =
        firestore
            .collection(COLLECTION_PACKAGES)
            .whereEqualTo(FIELD_DESTINATION_ID, destinationId)
            .get()
            .await()
            .toObjects(TourPackage::class.java)

    suspend fun getPublishedPackages(): List<TourPackage> = firestore
        .collection(COLLECTION_PACKAGES)
        .whereEqualTo(FIELD_STATUS, CatalogStatus.PUBLICADO.name)
        .get()
        .await()
        .toObjects(TourPackage::class.java)

    suspend fun getPackage(packageId: String): TourPackage? =
        firestore.collection(COLLECTION_PACKAGES).document(packageId)
            .get().await()
            .toObject(TourPackage::class.java)

    /**
     * Resuelve los destinos referenciados por los paquetes en bloques de 10,
     * que es el máximo permitido por `whereIn` en Firestore.
     */
    suspend fun getDestinationsByIds(destinationIds: List<String>): Map<String, TourDestination> {
        if (destinationIds.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, TourDestination>()
        destinationIds.distinct().chunked(FIRESTORE_WHERE_IN_LIMIT).forEach { chunk ->
            val snapshot = firestore
                .collection(DestinationCatalogRepository.COLLECTION_DESTINATIONS)
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()
            snapshot.toObjects(TourDestination::class.java).forEach { result[it.id] = it }
        }
        return result
    }

    suspend fun savePackage(tourPackage: TourPackage): TourPackage {
        val uid = auth.currentUser?.uid.orEmpty()
        val now = System.currentTimeMillis()
        val id = tourPackage.id.ifBlank {
            firestore.collection(COLLECTION_PACKAGES).document().id
        }
        val persisted = tourPackage.copy(
            id = id,
            durationDays = tourPackage.durationDays.coerceAtLeast(1),
            createdBy = tourPackage.createdBy.ifBlank { uid },
            createdAtMillis = tourPackage.createdAtMillis.takeIf { it > 0L } ?: now,
            updatedAtMillis = now
        )
        firestore.collection(COLLECTION_PACKAGES)
            .document(id)
            .set(persisted.toFirestoreMap())
            .await()
        return persisted
    }

    suspend fun updateStatus(packageId: String, status: CatalogStatus) {
        firestore.collection(COLLECTION_PACKAGES)
            .document(packageId)
            .update(
                mapOf(
                    FIELD_STATUS to status.name,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )
            )
            .await()
    }

    /**
     * Publica los servicios incluidos en la subcolección
     * `packages/{packageId}/included_services` para poder auditarlos
     * documento por documento desde la consola de Firebase.
     */
    suspend fun replaceIncludedServices(
        packageId: String,
        serviceIds: List<String>
    ) {
        val subcollection = firestore.collection(COLLECTION_PACKAGES)
            .document(packageId)
            .collection(SUBCOLLECTION_SERVICES)

        val snapshot = subcollection.get().await()
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        serviceIds.forEach { serviceId ->
            batch.set(subcollection.document(serviceId), mapOf("serviceId" to serviceId))
        }
        batch.commit().await()
    }

    suspend fun getIncludedServiceIds(packageId: String): List<String> =
        firestore.collection(COLLECTION_PACKAGES)
            .document(packageId)
            .collection(SUBCOLLECTION_SERVICES)
            .get()
            .await()
            .documents
            .mapNotNull { it.getString("serviceId") }

    companion object {
        const val COLLECTION_PACKAGES = "packages"
        const val SUBCOLLECTION_SERVICES = "included_services"
        const val FIELD_DESTINATION_ID = "destinationId"
        const val FIELD_STATUS = "status"
        const val FIELD_UPDATED_AT = "updatedAtMillis"
        private const val FIRESTORE_WHERE_IN_LIMIT = 10
    }
}
