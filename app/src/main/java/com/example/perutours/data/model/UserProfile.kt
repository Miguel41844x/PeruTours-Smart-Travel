package com.example.perutours.data.model

import com.google.firebase.firestore.Exclude

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dni: String = "",
    val city: String = "Lima, Perú",
    val role: String = "cliente",
    val photoUrl: String = "",
    val preferences: List<String> = emptyList(),

    // HU10: preferencias estructuradas para el servicio de recomendación
    val tripType: String = "",
    val budget: String = "",
    val duration: String = ""
) {
    @get:Exclude
    val userRole: UserRole
        get() = UserRole.fromWire(role)
}
