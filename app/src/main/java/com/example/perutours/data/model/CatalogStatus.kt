package com.example.perutours.data.model

/**
 * Estados del ciclo de vida del catálogo turístico.
 *
 * Proceso de gestión de catálogo (HU admin):
 * BORRADOR -> PUBLICADO -> ARCHIVADO
 *
 * El cliente solo puede ver elementos en estado PUBLICADO; las reglas de
 * Firestore replican esta condición para no depender del filtro de la app.
 */
enum class CatalogStatus {
    BORRADOR,
    PUBLICADO,
    ARCHIVADO;

    companion object {
        /** Evita lanzar excepciones cuando un documento llega con un valor inesperado. */
        fun from(raw: String?): CatalogStatus =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: BORRADOR
    }
}
