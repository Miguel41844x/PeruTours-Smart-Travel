package com.example.perutours.data.repository

import android.net.Uri
import com.example.perutours.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getProfile(uid: String): UserProfile {

        val currentUser = auth.currentUser

        // Datos iniciales desde FirebaseAuth
        val rawDisplay = currentUser?.displayName ?: ""

        val parts = rawDisplay
            .split("|")
            .map { it.trim() }

        val initialName = parts.getOrNull(0) ?: ""
        val initialRole = parts.getOrNull(1) ?: "cliente"
        val initialPhone = parts.getOrNull(2) ?: ""

        var profile = UserProfile(
            uid = uid,
            name = initialName,
            email = currentUser?.email ?: "",
            phone = initialPhone,
            role = initialRole,
            photoUrl = currentUser?.photoUrl?.toString() ?: ""
        )

        // Intentamos recuperar los datos guardados en Firestore
        val document = firestore
            .collection("users")
            .document(uid)
            .get()
            .await()

        if (document.exists()) {

            val savedPreferences =
                document.get("preferences") as? List<*>

            profile = profile.copy(
                name = document.getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.name,

                email = document.getString("email")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.email,

                phone = document.getString("phone")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.phone,

                dni = document.getString("dni")
                    ?: "",

                city = document.getString("city")
                    ?.takeIf { it.isNotBlank() }
                    ?: "Lima, Perú",

                role = document.getString("role")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.role,

                photoUrl = document.getString("photoUrl")
                    ?.takeIf { it.isNotBlank() }
                    ?: profile.photoUrl,

                preferences = savedPreferences
                    ?.filterIsInstance<String>()
                    ?: emptyList()
            )
        }

        return profile
    }

    suspend fun saveProfile(profile: UserProfile) {

        val userProfileData = hashMapOf(
            "uid" to profile.uid,
            "name" to profile.name,
            "email" to profile.email,
            "phone" to profile.phone,
            "dni" to profile.dni,
            "city" to profile.city,
            "role" to profile.role,
            "photoUrl" to profile.photoUrl,
            "preferences" to profile.preferences,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore
            .collection("users")
            .document(profile.uid)
            .set(
                userProfileData,
                SetOptions.merge()
            )
            .await()

        // Mantener compatibilidad con HomeScreen
        val currentUser = auth.currentUser

        val profileUpdates = userProfileChangeRequest {
            displayName =
                "${profile.name} | ${profile.role} | ${profile.phone}"
        }

        currentUser
            ?.updateProfile(profileUpdates)
            ?.await()
    }

    suspend fun uploadProfilePhoto(
        uid: String,
        uri: Uri
    ): String {

        val photoRef = storage
            .reference
            .child("profile_pictures/$uid.jpg")

        // Subir imagen
        photoRef
            .putFile(uri)
            .await()

        // Obtener URL
        val downloadUri = photoRef
            .downloadUrl
            .await()

        val remoteUrl = downloadUri.toString()

        // Actualizar FirebaseAuth
        val profileUpdates = userProfileChangeRequest {
            photoUri = downloadUri
        }

        auth.currentUser
            ?.updateProfile(profileUpdates)
            ?.await()

        // Guardar URL en Firestore
        firestore
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "photoUrl" to remoteUrl
                ),
                SetOptions.merge()
            )
            .await()

        return remoteUrl
    }
}