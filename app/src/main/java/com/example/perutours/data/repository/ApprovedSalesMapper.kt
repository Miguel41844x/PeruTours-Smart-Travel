package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.domain.analytics.ApprovedSaleRecord
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale

/**
 * Un pago aprobado = una venta. Este es el único criterio de "ingreso" de la app:
 * lo usa el dashboard económico del gerente (HU09).
 * La fecha de la venta es la de aprobación del pago.
 */
internal fun DocumentSnapshot.toApprovedSaleOrNull(quotation: DocumentSnapshot? = null): ApprovedSaleRecord? {
    if (getString("status") != Payment.STATUS_APPROVED) return null
    val amount = getDouble("amount") ?: return null
    if (!amount.isFinite() || amount <= 0.0) return null
    val currency = getString("currency").orEmpty().trim().uppercase(Locale.ROOT).ifBlank { "PEN" }
    val quotationCurrency = quotation?.getString("currency").orEmpty().trim().uppercase(Locale.ROOT)
    val quotedTotal = quotation?.getDouble("totalAmount")
    // Un costo ausente no equivale a cero ni debe inflar la rentabilidad.
    val estimatedCost = quotation?.getDouble("baseCost")?.takeIf { cost ->
        cost.isFinite() && cost > 0 && quotationCurrency == currency &&
            quotedTotal != null && quotedTotal.isFinite() && kotlin.math.abs(quotedTotal - amount) < 0.01
    }
    return ApprovedSaleRecord(
        amount = amount,
        currency = currency,
        approvedAtMillis = getLong("reviewedAtMillis")?.takeIf { it > 0L }
            ?: getLong("submittedAtMillis")?.takeIf { it > 0L }
            ?: 0L,
        estimatedCost = estimatedCost
    )
}
