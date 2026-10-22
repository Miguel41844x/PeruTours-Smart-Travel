package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogStatus
import com.example.perutours.data.model.TourDestination
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CRUD de destinos sobre Firestore (colección `destinations`).
 *
 * Solo el rol administrador puede escribir; las reglas de Firestore lo
 * verifican contra el documento del usuario. Aquí únicamente se modela el
 * proceso de negocio: registrar -> validar coordenadas -> publicar.
 */
class DestinationCatalogRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun getAllDestinations(): List<TourDestination> = firestore
        .collection(COLLECTION_DESTINATIONS)
        .orderBy(FIELD_NAME)
        .get()
        .await()
        .toObjects(TourDestination::class.java)

    /** Destinos que el cliente puede ver. */
    suspend fun getPublishedDestinations(): List<TourDestination> = firestore
        .collection(COLLECTION_DESTINATIONS)
        .whereEqualTo(FIELD_STATUS, CatalogStatus.PUBLICADO.name)
        .get()
        .await()
        .toObjects(TourDestination::class.java)

    suspend fun getDestination(destinationId: String): TourDestination? =
        firestore.collection(COLLECTION_DESTINATIONS).document(destinationId)
            .get().await()
            .toObject(TourDestination::class.java)

    /** Crea o actualiza el destino y devuelve el documento persistido. */
    suspend fun saveDestination(destination: TourDestination): TourDestination {
        val uid = auth.currentUser?.uid.orEmpty()
        val now = System.currentTimeMillis()
        val id = destination.id.ifBlank {
            firestore.collection(COLLECTION_DESTINATIONS).document().id
        }
        val persisted = destination.copy(
            id = id,
            // El creador no se pisa al editar un destino existente.
            createdBy = destination.createdBy.ifBlank { uid },
            createdAtMillis = destination.createdAtMillis.takeIf { it > 0L } ?: now,
            updatedAtMillis = now
        )
        firestore.collection(COLLECTION_DESTINATIONS)
            .document(id)
            .set(persisted.toFirestoreMap())
            .await()
        return persisted
    }

    suspend fun updateStatus(destinationId: String, status: CatalogStatus) {
        firestore.collection(COLLECTION_DESTINATIONS)
            .document(destinationId)
            .update(
                mapOf(
                    FIELD_STATUS to status.name,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )
            )
            .await()
    }

    companion object {
        const val COLLECTION_DESTINATIONS = "destinations"
        const val FIELD_STATUS = "status"
        const val FIELD_UPDATED_AT = "updatedAtMillis"
        private const val FIELD_NAME = "name"
    }
}
