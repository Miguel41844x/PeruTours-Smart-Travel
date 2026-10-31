package com.example.perutours.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.example.perutours.data.model.Reservation

@Entity(tableName = "reservation_cache")
data class ReservationEntity(
    @PrimaryKey
    val id: String,
    val quotationId: String = "",
    val requestId: String = "",
    val clientId: String = "",
    val agentId: String = "",
    val destination: String = "",
    val originCity: String = "",
    val departureAtMillis: Long = 0L,
    val returnAtMillis: Long = 0L,
    val travelerCount: Int = 1,
    val totalAmount: Double = 0.0,
    val currency: String = "PEN",
    val validityDate: String = "",
    val conditions: String = "",
    val status: String = "",
    val createdAtMillis: Long = 0L,
    val paymentId: String = "",
    val paymentVoucherUrl: String = "",
    val paymentMethod: String = "",
    val paidAtMillis: Long? = null,
    val cachedAtMillis: Long = System.currentTimeMillis()
) {
    fun toReservation(): Reservation {
        return Reservation(
            id = id,
            quotationId = quotationId,
            requestId = requestId,
            clientId = clientId,
            agentId = agentId,
            destination = destination,
            originCity = originCity,
            departureAtMillis = departureAtMillis,
            returnAtMillis = returnAtMillis,
            travelerCount = travelerCount,
            totalAmount = totalAmount,
            currency = currency,
            validityDate = validityDate,
            conditions = conditions,
            status = status,
            createdAtMillis = createdAtMillis,
            paymentId = paymentId,
            paymentVoucherUrl = paymentVoucherUrl,
            paymentMethod = paymentMethod,
            paidAtMillis = paidAtMillis
        )
    }

    companion object {
        fun fromReservation(res: Reservation): ReservationEntity {
            return ReservationEntity(
                id = res.id,
                quotationId = res.quotationId,
                requestId = res.requestId,
                clientId = res.clientId,
                agentId = res.agentId,
                destination = res.destination,
                originCity = res.originCity,
                departureAtMillis = res.departureAtMillis,
                returnAtMillis = res.returnAtMillis,
                travelerCount = res.travelerCount,
                totalAmount = res.totalAmount,
                currency = res.currency,
                validityDate = res.validityDate,
                conditions = res.conditions,
                status = res.status,
                createdAtMillis = res.createdAtMillis,
                paymentId = res.paymentId,
                paymentVoucherUrl = res.paymentVoucherUrl,
                paymentMethod = res.paymentMethod,
                paidAtMillis = res.paidAtMillis
            )
        }
    }
}

@Dao
interface ReservationCacheDao {
    @Query("SELECT * FROM reservation_cache WHERE (:status = 'Todos' OR status = :status) ORDER BY createdAtMillis DESC LIMIT :limit")
    suspend fun getCachedReservations(status: String, limit: Int): List<ReservationEntity>

    @Upsert
    suspend fun cacheReservations(reservations: List<ReservationEntity>)

    @Query("DELETE FROM reservation_cache")
    suspend fun clearCache()
}
