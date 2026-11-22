package com.example.perutours.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.example.perutours.domain.payment.VoucherOcrResult
import com.example.perutours.domain.payment.VoucherTextParser
import com.example.perutours.domain.payment.VoucherTextLine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

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
            val bitmap = image.drawable.toBitmap().forTextRecognition()
            val recognized = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
            val lines = recognized.textBlocks.flatMap { it.lines }.mapNotNull { line ->
                line.boundingBox?.let { bounds ->
                    VoucherTextLine(line.text, bounds.left, bounds.top, bounds.right, bounds.bottom)
                }
            }
            VoucherTextParser.parse(recognized.text, lines)
        } finally {
            recognizer.close()
        }
    }

    /** Mantiene legibles los caracteres pequeños de capturas de baja resolución. */
    private fun Bitmap.forTextRecognition(): Bitmap {
        val longest = max(width, height)
        if (longest >= MIN_IMAGE_SIDE) return this
        val factor = MIN_IMAGE_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(
            this, (width * factor).roundToInt(), (height * factor).roundToInt(), true
        )
    }

    private companion object {
        const val MIN_IMAGE_SIDE = 1600
        const val MAX_IMAGE_SIDE = 2048
    }
}
