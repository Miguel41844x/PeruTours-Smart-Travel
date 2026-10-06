package com.example.perutours.data.repository

import android.net.Uri
import com.example.perutours.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.example.perutours.data.model.Preference

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    fun getAuthProfile(uid: String): UserProfile {
        val currentUser = auth.currentUser
        val parts = currentUser?.displayName
            .orEmpty()
            .split("|")
            .map { it.trim() }

        return UserProfile(
            uid = uid,
            name = parts.getOrNull(0).orEmpty(),
            email = currentUser?.email.orEmpty(),
            phone = parts.getOrNull(2).orEmpty(),
            role = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: "cliente",
            photoUrl = currentUser?.photoUrl?.toString().orEmpty()
        )
    }

    suspend fun getProfile(uid: String): UserProfile {
        var profile = getAuthProfile(uid)

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

    suspend fun getPreferences(): List<Preference> {

        val snapshot = firestore
            .collection("preferences")
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { document ->

                val name = document.getString("name")
                    ?: return@mapNotNull null

                Preference(
                    id = document.id,
                    name = name,
                    description = document.getString("description").orEmpty(),
                    active = document.getBoolean("active") ?: true,
                    order = document.getLong("order")?.toInt() ?: 0
                )
            }
            .filter { it.active }
            .sortedBy { it.order }
    }
}
