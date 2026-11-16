package com.example.perutours.data.ocr

import android.content.Context
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.example.perutours.domain.payment.VoucherOcrResult
import com.example.perutours.domain.payment.VoucherTextParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

fun interface VoucherTextReader {
    suspend fun read(uri: Uri): VoucherOcrResult
}

class MlKitVoucherTextReader(private val context: Context) : VoucherTextReader {
    override suspend fun read(uri: Uri): VoucherOcrResult = withContext(Dispatchers.Default) {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(MAX_IMAGE_SIDE)
            .scale(Scale.FIT)
            .allowHardware(false)
            .diskCachePolicy(CachePolicy.READ_ONLY)
            .build()
        val image = context.imageLoader.execute(request) as? SuccessResult
            ?: throw IllegalArgumentException("No se pudo abrir la imagen.")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val input = InputImage.fromBitmap(image.drawable.toBitmap(), 0)
            VoucherTextParser.parse(recognizer.process(input).await().text)
        } finally {
            recognizer.close()
        }
    }

    private companion object {
        const val MAX_IMAGE_SIDE = 1600
    }
}
