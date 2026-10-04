package com.example.perutours.data.model

data class CatalogService(
    val id: String,
    val name: String,
    val category: String,
    val unitCost: Double,
    val description: String,
    val defaultQuantity: Int = 1
)

object ServiceCatalogData {
    val predefinedServices = listOf(
        CatalogService(
            id = "cat_hotel_4s",
            name = "Hotel Casa Andina Premium 4★ (Hab. Doble x noche)",
            category = "Alojamiento",
            unitCost = 140.0,
            description = "Incluye desayuno buffet andino y calefacción oxigenada.",
            defaultQuantity = 3
        ),
        CatalogService(
            id = "cat_hotel_5s",
            name = "Hotel Tambo del Inka Luxury 5★ Valle Sagrado",
            category = "Alojamiento",
            unitCost = 260.0,
            description = "Resort de lujo con estación privada de tren hacia Machu Picchu.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_tren_visto",
            name = "Tren Panorámico Vistadome Ida y Vuelta",
            category = "Transporte",
            unitCost = 190.0,
            description = "Vagones con techo de cristal, show de danza en vivo y snack.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_tren_exp",
            name = "Tren Expedition Turístico Clásico",
            category = "Transporte",
            unitCost = 110.0,
            description = "Asientos cómodos con audio-guía paisajística.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_mp_ticket",
            name = "Ticket Oficial Llaqta Machu Picchu Circuito 2A",
            category = "Tours",
            unitCost = 80.0,
            description = "Ingreso oficial reservado ante Ministerio de Cultura.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_guia_priv",
            name = "Guía Privado Oficial Bilingüe en Machu Picchu (3h)",
            category = "Guía",
            unitCost = 65.0,
            description = "Historiador acreditado con titulación profesional de turismo.",
            defaultQuantity = 1
        ),
        CatalogService(
            id = "cat_valle_sagrado",
            name = "Tour Valle Sagrado VIP + Almuerzo Buffet Campestre",
            category = "Tours",
            unitCost = 95.0,
            description = "Pisac, Ollantaytambo, Chinchero y gastronomía tradicional.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_vinicunca",
            name = "Tour Montaña de 7 Colores (Vinicunca) con Desayuno",
            category = "Tours",
            unitCost = 75.0,
            description = "Transporte 4x4, bastones de trekking y balón de oxígeno.",
            defaultQuantity = 2
        ),
        CatalogService(
            id = "cat_seguro",
            name = "Seguro Médico de Viaje y Asistencia en Altura 24h",
            category = "Seguro",
            unitCost = 35.0,
            description = "Cobertura médica hasta $50,000 USD y evacuación inmediata.",
            defaultQuantity = 2
        )
    )
}