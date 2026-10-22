package com.example.perutours.data.repository

import com.example.perutours.data.model.TourProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * CRUD de proveedores sobre Firestore (colección `providers`).
 *
 * El administrador registra hoteles, transportistas y guías; un proveedor
 * desactivado deja de estar disponible para nuevos paquetes.
 */
class TourProviderRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    suspend fun getProviders(includeInactive: Boolean = false): List<TourProvider> {
        val query = firestore.collection(COLLECTION_PROVIDERS)
            .orderBy(FIELD_NAME)
        val snapshot = (if (includeInactive) query else query.whereEqualTo(FIELD_ACTIVE, true))
            .get().await()
        return snapshot.toObjects(TourProvider::class.java)
    }

    suspend fun saveProvider(provider: TourProvider): TourProvider {
        val now = System.currentTimeMillis()
        val isNew = provider.id.isBlank()
        val id = provider.id.ifBlank {
            firestore.collection(COLLECTION_PROVIDERS).document().id
        }
        val createdAtMillis = if (isNew) {
            now
        } else {
            provider.createdAtMillis.takeIf { it > 0L } ?: now
        }
        val persisted = provider.copy(
            id = id,
            createdBy = if (isNew) auth.currentUser?.uid.orEmpty() else provider.createdBy,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = if (isNew) createdAtMillis else now
        )
        firestore.collection(COLLECTION_PROVIDERS)
            .document(id)
            .set(persisted.toFirestoreMap())
            .await()
        return persisted
    }

    suspend fun setActive(providerId: String, active: Boolean) {
        firestore.collection(COLLECTION_PROVIDERS)
            .document(providerId)
            .update(
                mapOf(
                    FIELD_ACTIVE to active,
                    FIELD_UPDATED_AT to System.currentTimeMillis()
                )
            )
            .await()
    }

    companion object {
        const val COLLECTION_PROVIDERS = "providers"
        const val FIELD_NAME = "name"
        const val FIELD_ACTIVE = "active"
        const val FIELD_UPDATED_AT = "updatedAtMillis"
    }
}
