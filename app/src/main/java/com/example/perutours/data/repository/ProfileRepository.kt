package com.example.perutours.data.repository

import android.content.Context
import android.net.Uri
import com.example.perutours.data.model.Preference
import com.example.perutours.data.model.UserProfile
import com.example.perutours.data.model.UserPreference
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val context: Context? = null
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

        val document = firestore
            .collection("users")
            .document(uid)
            .get()
            .await()

        if (document.exists()) {
            val savedPreferences = document.get("preferences") as? List<*>

            // Intentar recuperar desde la relación lógica USER_PREFERENCE (subcolección)
            val subcollPrefs = try {
                val userPrefSnapshot = firestore
                    .collection("users")
                    .document(uid)
                    .collection("user_preferences")
                    .get()
                    .await()

                userPrefSnapshot.documents.map { it.id }
            } catch (_: Exception) {
                emptyList()
            }

            val finalPreferences = if (subcollPrefs.isNotEmpty()) {
                subcollPrefs
            } else {
                savedPreferences?.filterIsInstance<String>() ?: emptyList()
            }

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

                preferences = finalPreferences
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
            .set(userProfileData, SetOptions.merge())
            .await()

        // Guardar/sincronizar la relación lógica USER_PREFERENCE
        try {
            val userPrefRef = firestore
                .collection("users")
                .document(profile.uid)
                .collection("user_preferences")

            val currentDocs = userPrefRef.get().await()
            val existingIds = currentDocs.documents.map { it.id }.toSet()
            val newIds = profile.preferences.toSet()

            // Eliminar preferencias desmarcadas
            for (doc in currentDocs.documents) {
                if (doc.id !in newIds) {
                    userPrefRef.document(doc.id).delete().await()
                }
            }

            // Agregar/actualizar nuevas preferencias
            for (prefId in newIds) {
                if (prefId !in existingIds) {
                    val userPref = UserPreference(
                        userId = profile.uid,
                        preferenceId = prefId
                    )
                    userPrefRef.document(prefId).set(userPref).await()
                }
            }
        } catch (_: Exception) {
            // Se mantiene el guardado principal en el documento del usuario
        }

        // Mantener compatibilidad con FirebaseAuth displayName
        val currentUser = auth.currentUser
        val profileUpdates = userProfileChangeRequest {
            displayName = "${profile.name} | ${profile.role} | ${profile.phone}"
        }

        currentUser?.updateProfile(profileUpdates)?.await()
    }

    suspend fun uploadProfilePhoto(
        uid: String,
        uri: Uri
    ): String {
        // 1. Validar propietario
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.uid != uid) {
            throw IllegalArgumentException("El usuario autenticado no coincide con el propietario de la cuenta.")
        }

        // 2. Validar MIME y tamaño máximo en el cliente
        context?.let { ctx ->
            val mimeType = ctx.contentResolver.getType(uri)
            if (mimeType != null && !mimeType.startsWith("image/")) {
                throw IllegalArgumentException("El archivo seleccionado no es una imagen válida (JPEG, PNG, WEBP).")
            }

            val size = try {
                ctx.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (_: Exception) {
                0L
            }

            if (size > 5 * 1024 * 1024) {
                throw IllegalArgumentException("La imagen supera el tamaño máximo permitido de 5 MB.")
            }
        }

        val photoRef = storage
            .reference
            .child("profile_pictures/$uid.jpg")

        // Subir imagen a Storage
        photoRef.putFile(uri).await()

        // Obtener URL remota
        val downloadUri = photoRef.downloadUrl.await()
        val remoteUrl = downloadUri.toString()

        // Actualizar FirebaseAuth
        val profileUpdates = userProfileChangeRequest {
            photoUri = downloadUri
        }
        currentUser.updateProfile(profileUpdates).await()

        // Guardar URL remota en Firestore
        firestore
            .collection("users")
            .document(uid)
            .set(
                mapOf("photoUrl" to remoteUrl),
                SetOptions.merge()
            )
            .await()

        return remoteUrl
    }

    fun observeUserProfile(uid: String): Flow<UserProfile> = callbackFlow {
        if (uid.isBlank()) {
            trySend(getAuthProfile(""))
            close()
            return@callbackFlow
        }

        val registration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val authProfile = getAuthProfile(uid)
                    val embeddedPrefs = (snapshot.get("preferences") as? List<*>)
                        ?.filterIsInstance<String>()
                        .orEmpty()

                    firestore.collection("users").document(uid)
                        .collection("user_preferences")
                        .get()
                        .addOnSuccessListener { prefSnapshot ->
                            val subcollPrefs = prefSnapshot.documents.map { it.id }
                            val finalPrefs = if (subcollPrefs.isNotEmpty()) subcollPrefs else embeddedPrefs

                            val userProfile = authProfile.copy(
                                name = snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: authProfile.name,
                                email = snapshot.getString("email")?.takeIf { it.isNotBlank() } ?: authProfile.email,
                                phone = snapshot.getString("phone")?.takeIf { it.isNotBlank() } ?: authProfile.phone,
                                dni = snapshot.getString("dni") ?: "",
                                city = snapshot.getString("city")?.takeIf { it.isNotBlank() } ?: "Lima, Perú",
                                role = snapshot.getString("role")?.takeIf { it.isNotBlank() } ?: authProfile.role,
                                photoUrl = snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() } ?: authProfile.photoUrl,
                                preferences = finalPrefs
                            )
                            trySend(userProfile)
                        }
                        .addOnFailureListener {
                            val userProfile = authProfile.copy(
                                name = snapshot.getString("name")?.takeIf { it.isNotBlank() } ?: authProfile.name,
                                email = snapshot.getString("email")?.takeIf { it.isNotBlank() } ?: authProfile.email,
                                phone = snapshot.getString("phone")?.takeIf { it.isNotBlank() } ?: authProfile.phone,
                                dni = snapshot.getString("dni") ?: "",
                                city = snapshot.getString("city")?.takeIf { it.isNotBlank() } ?: "Lima, Perú",
                                role = snapshot.getString("role")?.takeIf { it.isNotBlank() } ?: authProfile.role,
                                photoUrl = snapshot.getString("photoUrl")?.takeIf { it.isNotBlank() } ?: authProfile.photoUrl,
                                preferences = embeddedPrefs
                            )
                            trySend(userProfile)
                        }
                }
            }

        awaitClose { registration.remove() }
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
