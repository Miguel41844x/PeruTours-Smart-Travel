package com.example.perutours.data.model

/**
 * Proveedor turístico del catálogo (hoteles, transportistas, guías, etc.).
 *
 * Colección Firestore: providers/{providerId}
 *
 * El administrador los mantiene activos/inactivos; un paquete solo puede
 * incluir servicios de proveedores activos.
 */
data class TourProvider(
    val id: String = "",
    val name: String = "",
    val contactName: String = "",
    val phone: String = "",
    val email: String = "",
    val city: String = "",
    val serviceNames: List<String> = emptyList(),
    val active: Boolean = true,
    val createdBy: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "contactName" to contactName,
        "phone" to phone,
        "email" to email,
        "city" to city,
        "serviceNames" to serviceNames,
        "active" to active,
        "createdBy" to createdBy,
        "createdAtMillis" to createdAtMillis,
        "updatedAtMillis" to updatedAtMillis
    )
}
