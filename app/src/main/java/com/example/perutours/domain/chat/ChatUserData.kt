package com.example.perutours.domain.chat

import com.example.perutours.data.model.Quotation
import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TravelRequest

/** Paquete del catálogo de PeruTours que el chatbot puede mencionar. */
data class ChatCatalogItem(
    val title: String,
    val location: String,
    val duration: String,
    val price: String,
    val category: String
)

/**
 * Todo lo que el chatbot sabe del cliente autenticado (HU11).
 * Se arma siempre con datos del propio usuario.
 */
data class ChatUserData(
    val userName: String = "",
    val requests: List<TravelRequest> = emptyList(),
    val quotations: List<Quotation> = emptyList(),
    val reservations: List<Reservation> = emptyList(),
    val catalog: List<ChatCatalogItem> = emptyList()
) {
    /**
     * Garantía de privacidad (doble barrera sobre la consulta a Firestore):
     * descarta cualquier documento que no pertenezca a [uid]. Con un uid
     * vacío no se conserva nada.
     */
    fun restrictedTo(uid: String): ChatUserData {
        if (uid.isBlank()) {
            return copy(
                requests = emptyList(),
                quotations = emptyList(),
                reservations = emptyList()
            )
        }
        return copy(
            requests = requests.filter { it.userId == uid },
            quotations = quotations.filter { it.clientId == uid },
            reservations = reservations.filter { it.clientId == uid }
        )
    }
}