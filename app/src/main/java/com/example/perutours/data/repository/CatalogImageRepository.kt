package com.example.perutours.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Subida de imágenes del catálogo a Firebase Storage.
 *
 * Estructura: destination_images/{destinationId}/{timestamp}.jpg
 * Las reglas de Storage restringen la escritura al administrador y limitan
 * el tamaño y el tipo de archivo.
 */
class CatalogImageRepository(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) {

    /**
     * Sube la imagen de portada de un destino y devuelve su URL pública.
     * Si el destino aún no existe, debe guardarse primero (borrador) para
     * tener un ID con el cual crear la ruta en Storage.
     */
    suspend fun uploadDestinationCover(destinationId: String, imageUri: Uri): String {
        require(destinationId.isNotBlank()) {
            "Guarda el destino como borrador antes de subir su imagen."
        }

        val destination = storage.reference
            .child(DESTINATION_IMAGES_PATH)
            .child(destinationId)
            .child("${System.currentTimeMillis()}_${auth.currentUser?.uid.orEmpty().ifBlank { "anon" }}.jpg")

        destination.putFile(imageUri).await()
        return destination.downloadUrl.await().toString()
    }

    /** Elimina una imagen previamente subida; ignora ausencias. */
    suspend fun deleteDestinationCover(imageUrl: String) {
        val reference = storage.getReferenceFromUrl(imageUrl)
        runCatching { reference.delete().await() }
    }

    companion object {
        const val DESTINATION_IMAGES_PATH = "destination_images"
    }
}
