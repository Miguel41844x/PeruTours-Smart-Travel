package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.data.model.SalesAnalytics
import com.example.perutours.domain.analytics.ApprovedSaleRecord
import com.example.perutours.domain.analytics.SalesAnalyticsCalculator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

interface SalesDashboardDataSource {
    suspend fun fetchSalesAnalytics(): SalesAnalytics
}

class SalesDashboardRepository(
    private val firestore: FirebaseFirestore,
    private val calculator: SalesAnalyticsCalculator
) : SalesDashboardDataSource {

    override suspend fun fetchSalesAnalytics(): SalesAnalytics = coroutineScope {
        val quotationsDeferred = async {
            firestore.collection(COLLECTION_QUOTATIONS).get().await()
        }
        val paymentsDeferred = async {
            firestore.collection(COLLECTION_PAYMENTS).get().await()
        }
        val requestsDeferred = async {
            firestore.collection(COLLECTION_REQUESTS).get().await()
        }

        val quotationStatuses = quotationsDeferred.await().documents.map { document ->
            document.getString("status").orEmpty()
        }
        val approvedSales = paymentsDeferred.await().documents.mapNotNull { document ->
            if (document.getString("status") != Payment.STATUS_APPROVED) {
                return@mapNotNull null
            }

            val amount = document.getDouble("amount") ?: return@mapNotNull null
            if (!amount.isFinite() || amount < 0.0) return@mapNotNull null

            ApprovedSaleRecord(
                amount = amount,
                currency = document.getString("currency").orEmpty().ifBlank { DEFAULT_CURRENCY },
                approvedAtMillis = document.getLong("reviewedAtMillis")
                    ?: document.getLong("submittedAtMillis")
                    ?: 0L
            )
        }
        val requestedDestinations = requestsDeferred.await().documents.mapNotNull { document ->
            document.getString("destination")?.takeIf { it.isNotBlank() }
        }

        calculator.calculate(
            quotationStatuses = quotationStatuses,
            approvedSales = approvedSales,
            requestedDestinations = requestedDestinations
        )
    }

    private companion object {
        const val COLLECTION_QUOTATIONS = "cotizaciones"
        const val COLLECTION_PAYMENTS = "payments"
        const val COLLECTION_REQUESTS = "travel_requests"
        const val DEFAULT_CURRENCY = "PEN"
    }
}
