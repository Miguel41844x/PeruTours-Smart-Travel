package com.example.perutours.data.repository

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileRepositoryTest {

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockStorage: FirebaseStorage
    private lateinit var mockStorageRef: StorageReference
    private lateinit var mockPhotoRef: StorageReference
    private lateinit var repository: ProfileRepository

    private val testUid = "user_123_test"

    @Before
    fun setUp() {
        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockFirestore = mockk(relaxed = true)
        mockStorage = mockk(relaxed = true)
        mockStorageRef = mockk(relaxed = true)
        mockPhotoRef = mockk(relaxed = true)

        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns testUid
        every { mockUser.email } returns "test@perutours.pe"
        every { mockUser.displayName } returns "Juan Pérez | cliente | 987654321"

        every { mockStorage.reference } returns mockStorageRef
        every { mockStorageRef.child("profile_pictures/$testUid.jpg") } returns mockPhotoRef

        repository = ProfileRepository(
            auth = mockAuth,
            firestore = mockFirestore,
            storage = mockStorage,
            context = null
        )
    }

    @Test
    fun getAuthProfile_parsesDisplayNameRoleAndPhoneCorrectly() {
        val authProfile = repository.getAuthProfile(testUid)

        assertEquals(testUid, authProfile.uid)
        assertEquals("Juan Pérez", authProfile.name)
        assertEquals("test@perutours.pe", authProfile.email)
        assertEquals("987654321", authProfile.phone)
        assertEquals("cliente", authProfile.role)
    }

    @Test
    fun getProfile_retrievesFirestoreDataAndUserPreferenceRelation() = runTest {
        val mockUserDocRef = mockk<DocumentReference>(relaxed = true)
        val mockUsersCollection = mockk<CollectionReference>(relaxed = true)
        val mockDocSnapshot = mockk<DocumentSnapshot>(relaxed = true)
        val mockUserPrefCollection = mockk<CollectionReference>(relaxed = true)
        val mockUserPrefQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
        val mockPrefDoc = mockk<QueryDocumentSnapshot>(relaxed = true)

        every { mockFirestore.collection("users") } returns mockUsersCollection
        every { mockUsersCollection.document(testUid) } returns mockUserDocRef

        val docTask: Task<DocumentSnapshot> = Tasks.forResult(mockDocSnapshot)
        every { mockUserDocRef.get() } returns docTask

        every { mockDocSnapshot.exists() } returns true
        every { mockDocSnapshot.getString("name") } returns "Juan Pérez Modificado"
        every { mockDocSnapshot.getString("email") } returns "test@perutours.pe"
        every { mockDocSnapshot.getString("phone") } returns "912345678"
        every { mockDocSnapshot.getString("dni") } returns "77654321"
        every { mockDocSnapshot.getString("city") } returns "Cusco, Perú"
        every { mockDocSnapshot.getString("role") } returns "cliente"
        every { mockDocSnapshot.getString("photoUrl") } returns "https://storage.test/photo.jpg"
        every { mockDocSnapshot.get("preferences") } returns listOf("pref_trekking", "pref_gastronomia")

        every { mockUserDocRef.collection("user_preferences") } returns mockUserPrefCollection
        val userPrefTask: Task<QuerySnapshot> = Tasks.forResult(mockUserPrefQuerySnapshot)
        every { mockUserPrefCollection.get() } returns userPrefTask
        every { mockUserPrefQuerySnapshot.documents } returns listOf<DocumentSnapshot>(mockPrefDoc)
        every { mockPrefDoc.id } returns "pref_trekking"

        val profile = repository.getProfile(testUid)

        assertEquals("Juan Pérez Modificado", profile.name)
        assertEquals("912345678", profile.phone)
        assertEquals("77654321", profile.dni)
        assertEquals("Cusco, Perú", profile.city)
        assertEquals(listOf("pref_trekking"), profile.preferences)
    }

    @Test
    fun uploadProfilePhoto_rejectsNonOwner() = runTest {
        val nonOwnerUid = "different_user_999"
        val mockUri = mockk<Uri>(relaxed = true)

        try {
            repository.uploadProfilePhoto(nonOwnerUid, mockUri)
            assertTrue("Debería fallar si el UID no coincide con el usuario autenticado", false)
        } catch (e: IllegalArgumentException) {
            assertEquals("El usuario autenticado no coincide con el propietario de la cuenta.", e.message)
        }
    }

    @Test
    fun getPreferences_returnsActivePreferencesOrdered() = runTest {
        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockQuerySnapshot = mockk<QuerySnapshot>(relaxed = true)
        val mockDoc1 = mockk<QueryDocumentSnapshot>(relaxed = true)
        val mockDoc2 = mockk<QueryDocumentSnapshot>(relaxed = true)

        every { mockFirestore.collection("preferences") } returns mockCollection
        val prefTask: Task<QuerySnapshot> = Tasks.forResult(mockQuerySnapshot)
        every { mockCollection.get() } returns prefTask

        every { mockQuerySnapshot.documents } returns listOf<DocumentSnapshot>(mockDoc1, mockDoc2)

        every { mockDoc1.id } returns "p1"
        every { mockDoc1.getString("name") } returns "Trekking"
        every { mockDoc1.getString("description") } returns "Caminatas en la montaña"
        every { mockDoc1.getBoolean("active") } returns true
        every { mockDoc1.getLong("order") } returns 2L

        every { mockDoc2.id } returns "p2"
        every { mockDoc2.getString("name") } returns "Gastronomía"
        every { mockDoc2.getString("description") } returns "Comida típica"
        every { mockDoc2.getBoolean("active") } returns true
        every { mockDoc2.getLong("order") } returns 1L

        val preferences = repository.getPreferences()

        assertEquals(2, preferences.size)
        assertEquals("p2", preferences[0].id)
        assertEquals("Gastronomía", preferences[0].name)
        assertEquals("p1", preferences[1].id)
        assertEquals("Trekking", preferences[1].name)
    }
}
