package com.example.perutours.data.repository

import android.util.Log
import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.QuotationObservation
import com.example.perutours.data.model.QuotationValidity
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class QuotationRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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

        // Validación financiera y de moneda/vigencia
        require(quotation.currency.equals("S/", ignoreCase = true) || quotation.currency.equals("PEN", ignoreCase = true) || quotation.currency.equals("USD", ignoreCase = true)) {
            "La moneda de la cotización debe ser S/ o PEN."
        }
        require(quotation.validUntilMillis > System.currentTimeMillis()) {
            "La fecha de vigencia debe ser futura."
        }
        require(quotation.services.isNotEmpty()) {
            "La cotización debe incluir al menos un servicio."
        }

        // Redondeo estricto a 2 decimales con BigDecimal
        val roundedServices = quotation.services.map { item ->
            val calcSubtotal = BigDecimal.valueOf(item.unitCost)
                .multiply(BigDecimal.valueOf(item.quantity.toLong()))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            item.copy(subtotal = calcSubtotal)
        }
        val roundedTotal = BigDecimal.valueOf(quotation.totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()
        val roundedBase = BigDecimal.valueOf(quotation.baseCost).setScale(2, RoundingMode.HALF_UP).toDouble()

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
            services = roundedServices,
            baseCost = roundedBase,
            totalAmount = roundedTotal,
            status = Quotation.STATUS_QUOTED,
            createdAtMillis = System.currentTimeMillis()
        )

        val batch = firestore.batch()

        // 1. Guardar documento principal de cotización
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

    suspend fun getCurrentClientQuotations(): List<Quotation> {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión para consultar tus cotizaciones.")

        return getQuotationsByClient(clientId)
            .sortedByDescending { it.createdAtMillis }
    }

    fun observeCurrentClientQuotations(): Flow<List<Quotation>> = callbackFlow {
        val clientId = auth.currentUser?.uid
        if (clientId == null) {
            close(IllegalStateException("Debes iniciar sesión para consultar tus cotizaciones."))
            return@callbackFlow
        }

        val listener = firestore.collection(COLLECTION_QUOTATIONS)
            .whereEqualTo("clientId", clientId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val quotations = snapshot
                    ?.toObjects(Quotation::class.java)
                    .orEmpty()
                    .sortedByDescending { quotation -> quotation.createdAtMillis }
                trySend(quotations)
            }

        awaitClose { listener.remove() }
    }

    suspend fun getQuotationById(quotationId: String): Quotation? {
        val snapshot = firestore.collection(COLLECTION_QUOTATIONS)
            .document(quotationId)
            .get()
            .await()
        return snapshot.toObject(Quotation::class.java)
    }

    suspend fun getQuotationByRequestId(requestId: String): Quotation? {
        if (requestId.isBlank()) return null
        val currentAgentId = auth.currentUser?.uid ?: return null
        val snapshot = firestore.collection(COLLECTION_QUOTATIONS)
            .whereEqualTo("requestId", requestId)
            .whereEqualTo("agentId", currentAgentId)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.toObject(Quotation::class.java)
    }

    suspend fun reviseObservedQuotation(revision: Quotation): Quotation {
        val currentAgentId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión para revisar la cotización.")
        require(revision.id.isNotBlank()) { "No se recibió el ID de la cotización." }
        require(revision.services.isNotEmpty()) { "La cotización debe incluir servicios." }
        require(revision.validUntilMillis > System.currentTimeMillis()) {
            "La nueva vigencia debe ser futura."
        }

        val quotationRef = firestore.collection(COLLECTION_QUOTATIONS).document(revision.id)

        return firestore.runTransaction { transaction ->
            val current = transaction.get(quotationRef)
                .toObject(Quotation::class.java)
                ?: throw IllegalStateException("No se encontró la cotización.")

            if (current.agentId != currentAgentId) {
                throw IllegalAccessException("La cotización pertenece a otro agente.")
            }
            if (current.status != Quotation.STATUS_OBSERVED) {
                throw IllegalStateException("La cotización no tiene cambios pendientes.")
            }

            val roundedServices = revision.services.map { item ->
                val calcSubtotal = BigDecimal.valueOf(item.unitCost)
                    .multiply(BigDecimal.valueOf(item.quantity.toLong()))
                    .setScale(2, RoundingMode.HALF_UP)
                    .toDouble()
                item.copy(subtotal = calcSubtotal)
            }
            val roundedTotal = BigDecimal.valueOf(revision.totalAmount).setScale(2, RoundingMode.HALF_UP).toDouble()
            val roundedBase = BigDecimal.valueOf(revision.baseCost).setScale(2, RoundingMode.HALF_UP).toDouble()

            // Manejo de versiones al corregir una cotización observada
            val newVersion = current.version + 1

            val updated = current.copy(
                services = roundedServices,
                baseCost = roundedBase,
                marginPercent = revision.marginPercent,
                totalAmount = roundedTotal,
                validityDate = revision.validityDate,
                validUntilMillis = revision.validUntilMillis,
                conditions = revision.conditions,
                status = Quotation.STATUS_QUOTED,
                version = newVersion,
                parentQuotationId = current.id
            )

            transaction.update(
                quotationRef,
                mapOf(
                    "services" to updated.services,
                    "baseCost" to updated.baseCost,
                    "marginPercent" to updated.marginPercent,
                    "totalAmount" to updated.totalAmount,
                    "validityDate" to updated.validityDate,
                    "validUntilMillis" to updated.validUntilMillis,
                    "conditions" to updated.conditions,
                    "status" to updated.status,
                    "version" to updated.version,
                    "parentQuotationId" to updated.parentQuotationId
                )
            )

            val requestRef = firestore.collection(COLLECTION_REQUESTS)
                .document(current.requestId)
            transaction.update(requestRef, "status", Quotation.STATUS_QUOTED)
            updated
        }.await()
    }

    suspend fun getCurrentClientQuotationById(quotationId: String): Quotation? {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión para consultar la cotización.")
        val quotation = getQuotationById(quotationId) ?: return null

        if (quotation.clientId != clientId) {
            throw IllegalAccessException("Esta cotización no pertenece al usuario autenticado.")
        }
        return quotation
    }

    suspend fun updateQuotationStatus(quotationId: String, newStatus: String): Boolean {
        firestore.collection(COLLECTION_QUOTATIONS)
            .document(quotationId)
            .update("status", newStatus)
            .await()
        return true
    }

    suspend fun cancelQuotation(quotationId: String): Quotation =
        updateClientDecision(
            quotationId = quotationId,
            newStatus = Quotation.STATUS_CANCELLED
        )

    suspend fun acceptQuotation(quotationId: String): Quotation {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión para aceptar la cotización.")
        val quotationRef = firestore.collection(COLLECTION_QUOTATIONS).document(quotationId)
        val reservationRef = firestore.collection(COLLECTION_RESERVATIONS).document(quotationId)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(quotationRef)
            val quotation = snapshot.toObject(Quotation::class.java)
                ?: throw IllegalStateException("No se encontró la cotización.")
            if (quotation.requestId.isBlank()) {
                throw IllegalStateException("La cotización no tiene una solicitud asociada.")
            }
            val requestRef = firestore.collection(COLLECTION_REQUESTS)
                .document(quotation.requestId)
            val travelRequest = transaction.get(requestRef)
                .toObject(TravelRequest::class.java)
                ?: throw IllegalStateException("No se encontró la solicitud de viaje.")

            if (quotation.clientId != clientId) {
                throw IllegalAccessException("Esta cotización no pertenece al usuario autenticado.")
            }
            if (!Quotation.canClientRespond(quotation.status)) {
                throw IllegalStateException("La cotización ya fue respondida.")
            }
            if (QuotationValidity.isExpired(quotation)) {
                throw IllegalStateException("La cotización ya venció.")
            }

            val respondedAt = System.currentTimeMillis()
            val reservation = Reservation(
                id = reservationRef.id,
                quotationId = quotation.id,
                requestId = quotation.requestId,
                clientId = quotation.clientId,
                agentId = quotation.agentId,
                destination = quotation.destination,
                originCity = travelRequest.originCity,
                departureAtMillis = travelRequest.departureAtMillis,
                returnAtMillis = travelRequest.returnAtMillis,
                travelerCount = quotation.travelerCount,
                services = quotation.services,
                totalAmount = quotation.totalAmount,
                currency = quotation.currency,
                validityDate = quotation.validityDate,
                conditions = quotation.conditions,
                createdAtMillis = respondedAt
            )

            transaction.set(reservationRef, reservation)
            transaction.update(
                quotationRef,
                mapOf(
                    "status" to Quotation.STATUS_ACCEPTED,
                    "respondedAtMillis" to respondedAt,
                    "reservationId" to reservation.id
                )
            )

            quotation.copy(
                status = Quotation.STATUS_ACCEPTED,
                respondedAtMillis = respondedAt,
                reservationId = reservation.id
            )
        }.await().also {
            // Segunda escritura: el estado de la solicitud viaja en una
            // segunda tanda. El evaluador de reglas Firestore tiene un techo
            // de 1000 expresiones COMPARTIDO por toda la solicitud, y las
            // tres escrituras en una sola tanda (reserva + cotización +
            // solicitud) agotaban ese presupuesto. Cada tanda por separado
            // valida igual (la regla de la solicitud exige que la
            // cotización ya esté 'Aceptada' con getAfter, que aquí lee el
            // resultado confirmado de la primera tanda).
            //
            // Si esta segunda tanda falla (p. ej. sin conexión), la
            // aceptación ya quedó registrada en la reserva y la cotización;
            // el estado de la solicitud se reintentará en la siguiente
            // sincronización, por lo que el error no se propaga.
            try {
                firestore.collection(COLLECTION_REQUESTS)
                    .document(it.requestId)
                    .update("status", Quotation.STATUS_ACCEPTED)
                    .await()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                Log.w("HU06", "Aceptación registrada, pero no se pudo actualizar el estado de la solicitud.")
            }
        }
    }

    suspend fun observeQuotation(quotationId: String, message: String): Quotation {
        val cleanMessage = message.trim()
        require(cleanMessage.isNotEmpty()) { "Escribe una observación antes de enviarla." }

        val currentUser = auth.currentUser
            ?: throw IllegalStateException("Debes iniciar sesión para observar la cotización.")
        val authorName = currentUser.displayName
            ?.substringBefore("|")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: currentUser.email.orEmpty()
        val quotationRef = firestore.collection(COLLECTION_QUOTATIONS).document(quotationId)
        val observationRef = quotationRef.collection(SUBCOLLECTION_OBSERVATIONS).document()

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(quotationRef)
            val quotation = snapshot.toObject(Quotation::class.java)
                ?: throw IllegalStateException("No se encontró la cotización.")

            if (quotation.clientId != currentUser.uid) {
                throw IllegalAccessException("Esta cotización no pertenece al usuario autenticado.")
            }
            if (!Quotation.canClientRespond(quotation.status)) {
                throw IllegalStateException("La cotización ya fue respondida.")
            }

            val respondedAt = System.currentTimeMillis()
            val observation = QuotationObservation(
                id = observationRef.id,
                quotationId = quotation.id,
                authorId = currentUser.uid,
                authorName = authorName,
                message = cleanMessage,
                createdAtMillis = respondedAt
            )

            transaction.set(observationRef, observation)
            transaction.update(
                quotationRef,
                mapOf(
                    "status" to Quotation.STATUS_OBSERVED,
                    "respondedAtMillis" to respondedAt,
                    "latestObservation" to cleanMessage
                )
            )

            if (quotation.requestId.isNotBlank()) {
                val requestRef = firestore.collection(COLLECTION_REQUESTS)
                    .document(quotation.requestId)
                transaction.update(requestRef, "status", Quotation.STATUS_OBSERVED)
            }

            quotation.copy(
                status = Quotation.STATUS_OBSERVED,
                respondedAtMillis = respondedAt,
                latestObservation = cleanMessage
            )
        }.await()
    }

    suspend fun getObservationHistory(quotationId: String): List<QuotationObservation> {
        val snapshot = firestore.collection(COLLECTION_QUOTATIONS)
            .document(quotationId)
            .collection(SUBCOLLECTION_OBSERVATIONS)
            .orderBy("createdAtMillis")
            .get()
            .await()

        return snapshot.toObjects(QuotationObservation::class.java)
    }

    private suspend fun updateClientDecision(
        quotationId: String,
        newStatus: String,
        latestObservation: String = ""
    ): Quotation {
        val clientId = auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión para responder la cotización.")
        val quotationRef = firestore.collection(COLLECTION_QUOTATIONS).document(quotationId)

        return firestore.runTransaction { transaction ->
            val snapshot = transaction.get(quotationRef)
            val quotation = snapshot.toObject(Quotation::class.java)
                ?: throw IllegalStateException("No se encontró la cotización.")

            if (quotation.clientId != clientId) {
                throw IllegalAccessException("Esta cotización no pertenece al usuario autenticado.")
            }
            if (!Quotation.canClientRespond(quotation.status)) {
                throw IllegalStateException("La cotización ya fue respondida.")
            }

            val respondedAt = System.currentTimeMillis()
            transaction.update(
                quotationRef,
                mapOf(
                    "status" to newStatus,
                    "respondedAtMillis" to respondedAt,
                    "latestObservation" to latestObservation
                )
            )

            if (quotation.requestId.isNotBlank()) {
                val requestRef = firestore.collection(COLLECTION_REQUESTS)
                    .document(quotation.requestId)
                transaction.update(requestRef, "status", newStatus)
            }

            quotation.copy(
                status = newStatus,
                respondedAtMillis = respondedAt,
                latestObservation = latestObservation
            )
        }.await()
    }

    companion object {
        const val COLLECTION_QUOTATIONS = "cotizaciones"
        const val COLLECTION_REQUESTS = "travel_requests"
        const val COLLECTION_RESERVATIONS = "reservations"
        const val SUBCOLLECTION_OBSERVATIONS = "observations"
    }
}
