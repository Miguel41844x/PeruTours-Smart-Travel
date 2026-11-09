package com.example.perutours.data.repository

import com.example.perutours.data.model.Payment
import com.example.perutours.domain.analytics.ApprovedSaleRecord
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Un pago aprobado = una venta. Este es el único criterio de "ingreso" de la app:
 * lo usan el dashboard del gerente (HU09) y el del administrador (HU08).
 * La fecha de la venta es la de aprobación del pago.
 */
internal fun DocumentSnapshot.toApprovedSaleOrNull(): ApprovedSaleRecord? {
    if (getString("status") != Payment.STATUS_APPROVED) return null
    val amount = getDouble("amount") ?: return null
    if (!amount.isFinite() || amount < 0.0) return null
    return ApprovedSaleRecord(
        amount = amount,
        currency = getString("currency").orEmpty().ifBlank { "PEN" },
        approvedAtMillis = getLong("reviewedAtMillis")
            ?: getLong("submittedAtMillis")
            ?: 0L
    )
}
