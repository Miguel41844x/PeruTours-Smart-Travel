package com.example.perutours.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object QuotationValidity {
    fun validUntilExclusiveMillis(validityDate: String): Long? {
        val parsedDate = runCatching {
            SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
                isLenient = false
            }.parse(validityDate.trim())
        }.getOrNull() ?: return null

        return Calendar.getInstance().apply {
            time = parsedDate
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }

    fun isExpired(validityDate: String, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val validUntil = validUntilExclusiveMillis(validityDate) ?: return true
        return nowMillis >= validUntil
    }

    private const val DATE_PATTERN = "yyyy-MM-dd"
}
