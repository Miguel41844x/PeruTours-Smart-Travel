package com.example.perutours.ui.screens.quotation

import com.example.perutours.data.model.CatalogService
import java.text.Normalizer
import java.util.Locale

/** Selects destination-specific services without offering another city's catalog. */
object QuotationCatalogFilter {

    fun forDestination(
        services: List<CatalogService>,
        destination: String
    ): List<CatalogService> {
        val destinationCity = destination.substringBefore(',').normalizePlaceName()
        if (destinationCity.isBlank()) return emptyList()

        return services.filter { service ->
            val serviceCity = service.city.normalizePlaceName()
            serviceCity.isBlank() || serviceCity == destinationCity
        }
    }

    fun destinationLabel(destination: String): String =
        destination.substringBefore(',').trim().ifBlank { "este destino" }

    private fun String.normalizePlaceName(): String = Normalizer
        .normalize(trim(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}
