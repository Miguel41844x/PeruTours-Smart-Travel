package com.example.perutours.data.repository

import com.example.perutours.data.model.TravelRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

interface TravelRequestDataSource {
    suspend fun save(request: TravelRequest): TravelRequest
    suspend fun getPendingRequests(): List<TravelRequest>
    suspend fun getAllRequests(): List<TravelRequest>
    suspend fun getRequestById(requestId: String): TravelRequest?
    suspend fun claimRequest(requestId: String): TravelRequest
}

class TravelRequestRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TravelRequestDataSource {
    override suspend fun save(request: TravelRequest): TravelRequest {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("No hay un usuario autenticado.")
        val document = firestore.collection(COLLECTION_NAME).document()
        val savedRequest = request.copy(
            id = document.id,
            userId = uid,
            status = TravelRequest.STATUS_PENDING_QUOTE,
            createdAtMillis = System.currentTimeMillis()
        )

        document.set(savedRequest).await()
        return savedRequest
    }

    override suspend fun getPendingRequests(): List<TravelRequest> {
        val agentId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión como agente.")
        return firestore
            .collection(COLLECTION_NAME)
            .whereEqualTo(
                "status",
                TravelRequest.STATUS_PENDING_QUOTE
            )
            .get()
            .await()
            .documents
            .mapNotNull {
                it.toObject(TravelRequest::class.java)
            }
            .filter { it.assignedAgentId.isBlank() || it.assignedAgentId == agentId }
    }

    override suspend fun getAllRequests(): List<TravelRequest> {
        val agentId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión como agente.")
        val ownedQuotations = firestore.collection("cotizaciones")
            .whereEqualTo("agentId", agentId)
            .get()
            .await()
            .documents
            .mapNotNull { it.toObject(com.example.perutours.data.model.Quotation::class.java) }
            .associateBy { it.requestId }
        val ownedRequestIds = ownedQuotations.keys
        return firestore
            .collection(COLLECTION_NAME)
            .get()
            .await()
            .documents
            .mapNotNull {
                it.toObject(TravelRequest::class.java)
            }
            .filter { request ->
                request.assignedAgentId == agentId ||
                    request.id in ownedRequestIds ||
                    (request.assignedAgentId.isBlank() &&
                        request.status == TravelRequest.STATUS_PENDING_QUOTE)
            }
            .map { request ->
                ownedQuotations[request.id]?.let { quotation ->
                    request.copy(
                        status = quotation.status,
                        quotationId = quotation.id,
                        assignedAgentId = quotation.agentId
                    )
                } ?: request
            }
            .sortedByDescending(TravelRequest::createdAtMillis)
    }

    override suspend fun getRequestById(requestId: String): TravelRequest? {
        if (requestId.isBlank()) return null

        val document = firestore
            .collection(COLLECTION_NAME)
            .document(requestId)
            .get()
            .await()

        return if (document.exists()) {
            document.toObject(TravelRequest::class.java)
        } else {
            null
        }
    }

    override suspend fun claimRequest(requestId: String): TravelRequest {
        require(requestId.isNotBlank()) { "No se recibió la solicitud." }
        val agentId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión como agente.")
        val reference = firestore.collection(COLLECTION_NAME).document(requestId)
        return firestore.runTransaction { transaction ->
            val current = transaction.get(reference).toObject(TravelRequest::class.java)
                ?: throw NoSuchElementException("La solicitud ya no existe.")
            if (current.status != TravelRequest.STATUS_PENDING_QUOTE) {
                throw IllegalStateException("La solicitud ya fue atendida.")
            }
            if (current.assignedAgentId.isNotBlank() && current.assignedAgentId != agentId) {
                throw IllegalStateException("Otro agente ya tomó esta solicitud.")
            }
            if (current.assignedAgentId == agentId) return@runTransaction current
            val claimedAt = System.currentTimeMillis()
            transaction.update(
                reference,
                mapOf(
                    "assignedAgentId" to agentId,
                    "claimedAtMillis" to claimedAt
                )
            )
            current.copy(assignedAgentId = agentId, claimedAtMillis = claimedAt)
        }.await()
    }

    companion object {
        const val COLLECTION_NAME = "travel_requests"
    }
}
