package com.example.perutours.data.model

data class QuotationObservation(
    val id: String = "",
    val quotationId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val message: String = "",
    val resultingStatus: String = Quotation.STATUS_OBSERVED,
    val createdAtMillis: Long = System.currentTimeMillis()
)
