package com.example.perutours.data.model

/**
 * Destino turístico ofrecido por PeruTours.
 *
 * Colección Firestore: destinations/{destinationId}
 *
 * Es el "lugar" del catálogo: su nombre y departamento se validan contra la
 * geocodificación externa para fijar coordenadas reales (latitud/longitud)
 * que después alimentan la consulta de puntos de interés cercanos.
 */
data class TourDestination(
    val id: String = "",
    val name: String = "",
    val department: String = "",
    val summary: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val coverImageUrl: String = "",
    val categories: List<String> = emptyList(),
    val externalPlaceId: String = "",
    val status: String = CatalogStatus.BORRADOR.name,
    val createdBy: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val published: Boolean
        get() = CatalogStatus.from(status) == CatalogStatus.PUBLICADO

    val hasCoordinates: Boolean
        get() = latitude != 0.0 || longitude != 0.0

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "department" to department,
        "summary" to summary,
        "latitude" to latitude,
        "longitude" to longitude,
        "coverImageUrl" to coverImageUrl,
        "categories" to categories,
        "externalPlaceId" to externalPlaceId,
        "status" to status,
        "createdBy" to createdBy,
        "createdAtMillis" to createdAtMillis,
        "updatedAtMillis" to updatedAtMillis
    )
}
