package com.example.perutours.data.remote.wikipedia

/**
 * Respuesta de la API REST de resúmenes de Wikipedia
 * (es.wikipedia.org/api/rest_v1/page/summary/{título}).
 */
data class WikipediaSummaryDto(
    val title: String = "",
    val extract: String = "",
    val description: String? = null,
    val thumbnail: WikipediaThumbnail? = null
)

data class WikipediaThumbnail(
    val source: String = "",
    val width: Int = 0,
    val height: Int = 0
)
