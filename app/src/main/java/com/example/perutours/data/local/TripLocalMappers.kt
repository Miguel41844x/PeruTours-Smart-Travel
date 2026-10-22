package com.example.perutours.data.local

import com.example.perutours.data.model.Reservation
import com.example.perutours.data.model.TripChecklistItem
import com.example.perutours.data.model.TripIncident
import com.example.perutours.data.model.TripItineraryItem

fun Reservation.toTripEntity(cachedAtMillis: Long = System.currentTimeMillis()) = TripEntity(
    reservationId = id,
    quotationId = quotationId,
    clientId = clientId,
    agentId = agentId,
    destination = destination,
    originCity = originCity,
    departureAtMillis = departureAtMillis,
    returnAtMillis = returnAtMillis,
    travelerCount = travelerCount,
    totalAmount = totalAmount,
    currency = currency,
    reservationStatus = status,
    cachedAtMillis = cachedAtMillis
)

fun TripEntity.toReservation() = Reservation(
    id = reservationId,
    quotationId = quotationId,
    clientId = clientId,
    agentId = agentId,
    destination = destination,
    originCity = originCity,
    departureAtMillis = departureAtMillis,
    returnAtMillis = returnAtMillis,
    travelerCount = travelerCount,
    totalAmount = totalAmount,
    currency = currency,
    status = reservationStatus
)

fun TripItineraryItem.toEntity() = TripItineraryEntity(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    agentId = agentId,
    title = title,
    description = description,
    category = category,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    startAtMillis = startAtMillis,
    endAtMillis = endAtMillis,
    dayIndex = dayIndex,
    sortOrder = sortOrder,
    providerName = providerName,
    status = status,
    notes = notes,
    source = source,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun TripItineraryEntity.toModel() = TripItineraryItem(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    agentId = agentId,
    title = title,
    description = description,
    category = category,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    startAtMillis = startAtMillis,
    endAtMillis = endAtMillis,
    dayIndex = dayIndex,
    sortOrder = sortOrder,
    providerName = providerName,
    status = status,
    notes = notes,
    source = source,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun TripChecklistItem.toEntity(
    syncState: String = TripChecklistEntity.SYNCED,
    isDeleted: Boolean = false
) = TripChecklistEntity(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    title = title,
    category = category,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    syncState = syncState,
    isDeleted = isDeleted
)

fun TripChecklistEntity.toModel() = TripChecklistItem(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    title = title,
    category = category,
    isCompleted = isCompleted,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun TripIncident.toEntity() = TripIncidentEntity(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    agentId = agentId,
    category = category,
    priority = priority,
    status = status,
    description = description,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    agentResponse = agentResponse,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    resolvedAtMillis = resolvedAtMillis
)

fun TripIncidentEntity.toModel() = TripIncident(
    id = id,
    reservationId = reservationId,
    clientId = clientId,
    agentId = agentId,
    category = category,
    priority = priority,
    status = status,
    description = description,
    locationName = locationName,
    latitude = latitude,
    longitude = longitude,
    agentResponse = agentResponse,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    resolvedAtMillis = resolvedAtMillis
)
