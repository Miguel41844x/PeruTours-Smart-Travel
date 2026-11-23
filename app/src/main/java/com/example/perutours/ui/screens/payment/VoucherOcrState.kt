package com.example.perutours.ui.screens.payment

import android.net.Uri
import com.example.perutours.data.ocr.VoucherTextReader
import com.example.perutours.domain.payment.VoucherOcrResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

sealed interface VoucherOcrState {
    data object Idle : VoucherOcrState
    data object Reading : VoucherOcrState
    data object NoData : VoucherOcrState
    data object Unavailable : VoucherOcrState
    data class Read(val result: VoucherOcrResult) : VoucherOcrState
}

internal suspend fun VoucherTextReader.readForReview(uri: Uri): VoucherOcrState = try {
    val result = withTimeoutOrNull(15_000) { read(uri) }
    when {
        result == null -> VoucherOcrState.Unavailable
        !result.hasData -> VoucherOcrState.NoData
        else -> VoucherOcrState.Read(result)
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    VoucherOcrState.Unavailable
}
