package com.example.perutours.data.model

import com.google.firebase.firestore.Exclude

data class TravelRequest(
    val id: String = "",
    val userId: String = "",
    val requestType: String = TravelRequestType.CUSTOM.wireValue,
    val packageId: String = "",
    val destinationId: String = "",
    val packageSnapshot: TravelPackageSnapshot = TravelPackageSnapshot(),
    val requestedServiceIds: List<String> = emptyList(),
    val destination: String = "",
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val originCity: String = "",
    val originLatitude: Double? = null,
    val originLongitude: Double? = null,
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val notes: String = "",
    val status: String = STATUS_PENDING_QUOTE,
    val createdAtMillis: Long = 0L,
    val quotationId: String = "",
    val assignedAgentId: String = "",
    val claimedAtMillis: Long? = null
) {
    @get:Exclude
    val isPackageRequest: Boolean
        get() = TravelRequestType.fromWire(requestType) == TravelRequestType.PACKAGE &&
            packageId.isNotBlank()

    @get:Exclude
    val workflowStatus: TravelRequestStatus
        get() = TravelRequestStatus.fromWire(status)

    companion object {
        const val STATUS_PENDING_QUOTE = "Pendiente de cotizacion"
        const val MAX_REQUESTED_ACTIVITIES = 5
    }
}
