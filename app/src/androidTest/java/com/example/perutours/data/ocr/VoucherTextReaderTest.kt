package com.example.perutours.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.perutours.domain.payment.VoucherAmount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class VoucherTextReaderTest {
    @Test
    fun readsOperationInAnotherColumnWithoutConfusingTheSecurityCode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("voucher_columns_test", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(1000, 900, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 38f
            }
            canvas.drawColor(Color.WHITE)
            canvas.drawText("Comprobante de prueba", 45f, 100f, paint)
            canvas.drawText("S/ 12.00", 45f, 220f, paint)
            canvas.drawText("Codigo de seguridad", 45f, 400f, paint)
            canvas.drawText("8456", 750f, 400f, paint)
            canvas.drawText("Nro. de operacion", 45f, 650f, paint)
            canvas.drawText("00123456", 700f, 650f, paint)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            val result = MlKitVoucherTextReader(context).read(Uri.fromFile(file))
            assertEquals(VoucherAmount(BigDecimal("12.00"), "PEN"), result.amount)
            assertEquals("00123456", result.operationNumber)
        } finally { file.delete() }
    }

    @Test
    fun readsAnAmountAndOperationFromAnImage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("voucher_ocr_test", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(1100, 500, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 64f
            }
            canvas.drawColor(Color.WHITE)
            canvas.drawText("Comprobante de prueba", 45f, 110f, paint)
            canvas.drawText("S/ 150.50", 45f, 225f, paint)
            canvas.drawText("Numero de operacion: 731546", 45f, 350f, paint)
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()

            val result = MlKitVoucherTextReader(context).read(Uri.fromFile(file))

            assertEquals(VoucherAmount(BigDecimal("150.50"), "PEN"), result.amount)
            assertEquals("731546", result.operationNumber)
        } finally {
            file.delete()
        }
    }
}
