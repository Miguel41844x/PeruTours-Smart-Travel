package com.example.perutours.data.repository

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.TravelRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.perutours.data.repository.ProfileRepository
class QuotationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * HU05: Guarda la cotización en Firestore en la colección 'cotizaciones'
     * con estado 'Cotizado' y actualiza atómicamente la solicitud en 'travel_requests'
     * al estado 'Cotizado' utilizando un WriteBatch.
     */
    suspend fun saveQuotation(quotation: Quotation): Quotation {
        val currentAgent = auth.currentUser
            ?: throw IllegalStateException(
                "No hay un agente autenticado."
            )

        val agentUid = currentAgent.uid
        val agentEmail = currentAgent.email ?: ""

        val agentDocument = firestore
            .collection("users")
            .document(agentUid)
            .get()
            .await()

        val agentName =
            agentDocument.getString("name")
                ?.takeIf { it.isNotBlank() }
                ?: agentEmail

        val docRef = firestore.collection(COLLECTION_QUOTATIONS).document()
        val quotationId = docRef.id

        val quotationToSave = quotation.copy(
            id = quotationId,
            agentId = agentUid,
            agentName = agentName,
            status = Quotation.STATUS_QUOTED,
            createdAtMillis = System.currentTimeMillis()
        )

        val batch = firestore.batch()

        // 1. Guardar documento de cotización
        batch.set(docRef, quotationToSave)

        // 2. Actualizar estado de la solicitud en 'travel_requests'
        if (quotation.requestId.isNotBlank()) {
            val requestRef = firestore.collection(COLLECTION_REQUESTS).document(quotation.requestId)
            batch.update(
                requestRef,
                mapOf(
                    "status" to Quotation.STATUS_QUOTED,
                    "quotationId" to quotationId
                )
            )
        }

        // 3. Registrar log de evento de Notificación Push FCM
        val fcmLogRef = firestore.collection(COLLECTION_FCM_LOGS).document()
        batch.set(
            fcmLogRef,
            mapOf(
                "toToken" to quotation.clientFcmToken,
                "title" to "¡Tu cotización para ${quotation.destination} está lista! ✈️",
                "body" to "El agente preparó tu propuesta por $${quotation.totalAmount} USD.",
                "quotationId" to quotationId,
                "status" to Quotation.STATUS_QUOTED,
                "timestamp" to System.currentTimeMillis()
            )
        )

        batch.commit().await()
        return quotationToSave
    }

    suspend fun getQuotationsByClient(clientId: String): List<Quotation> {
        val snapshot = firestore.collection(COLLECTION_QUOTATIONS)
            .whereEqualTo("clientId", clientId)
            .get()
            .await()
        return snapshot.toObjects(Quotation::class.java)
    }

    suspend fun getQuotationById(quotationId: String): Quotation? {
        val snapshot = firestore.collection(COLLECTION_QUOTATIONS)
            .document(quotationId)
            .get()
            .await()
        return snapshot.toObject(Quotation::class.java)
    }

    suspend fun updateQuotationStatus(quotationId: String, newStatus: String): Boolean {
        firestore.collection(COLLECTION_QUOTATIONS)
            .document(quotationId)
            .update("status", newStatus)
            .await()
        return true
    }

    companion object {
        const val COLLECTION_QUOTATIONS = "cotizaciones"
        const val COLLECTION_REQUESTS = "travel_requests"
        const val COLLECTION_FCM_LOGS = "fcm_push_logs"
    }
}
