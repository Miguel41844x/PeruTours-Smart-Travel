package com.example.perutours.ui.screens.payment

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.perutours.domain.payment.VoucherDate
import com.example.perutours.domain.payment.VoucherOcrResult
import com.example.perutours.domain.payment.VoucherTime
import com.example.perutours.ui.components.VoucherOcrPanel
import org.junit.Rule
import org.junit.Test

class VoucherOcrDateTimeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsThePrintedDateAndConvertedTimeEvenWhenOtherFieldsNeedReview() {
        compose.setContent {
            MaterialTheme {
                VoucherOcrPanel(
                    state = VoucherOcrState.Read(VoucherOcrResult(
                        amount = null, operationNumber = null,
                        transactionDate = VoucherDate(2026, 11, 21),
                        transactionTime = VoucherTime(19, 25)
                    )),
                    expectedAmount = 12.0, expectedCurrency = "PEN", onRead = {}
                )
            }
        }
        compose.onNodeWithText("Fecha detectada: 21/11/2026").assertIsDisplayed()
        compose.onNodeWithText("Hora detectada: 19:25").assertIsDisplayed()
        compose.onNodeWithText("Importe por verificar en la imagen.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar lectura").assertIsDisplayed()
    }

    @Test
    fun keepsAMissingTimePendingInsteadOfDisplayingMidnight() {
        compose.setContent {
            MaterialTheme {
                VoucherOcrPanel(
                    state = VoucherOcrState.Read(VoucherOcrResult(
                        amount = null, operationNumber = null,
                        transactionDate = VoucherDate(2026, 11, 21)
                    )),
                    expectedAmount = null, expectedCurrency = "PEN"
                )
            }
        }
        compose.onNodeWithText("Fecha detectada: 21/11/2026").assertIsDisplayed()
        compose.onNodeWithText("Hora por verificar en la imagen.").assertIsDisplayed()
    }
}
