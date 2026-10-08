package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ServiceCatalogRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun getServices(): List<CatalogService> {
        val snapshot = firestore
            .collection(COLLECTION_SERVICES)
            .whereEqualTo("active", true)
            .get()
            .await()

        return snapshot.toObjects(CatalogService::class.java)
            .sortedWith(
                compareBy<CatalogService> { it.city }
                    .thenBy { it.category }
                    .thenBy { it.name }
            )
    }

    suspend fun syncCatalog(){

        val services = listOf(

            // =========================
            // LIMA
            // =========================

            CatalogService(
                id = "lima_city_tour",
                name = "City Tour Lima",
                city = "Lima",
                category = "Tours",
                unitCost = 45.0,
                description = "Recorrido por los principales atractivos turísticos de Lima.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_miraflores_barranco",
                name = "Tour Miraflores y Barranco",
                city = "Lima",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido turístico por Miraflores, Barranco y sus principales atractivos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_centro_historico",
                name = "Centro Histórico de Lima",
                city = "Lima",
                category = "Tours",
                unitCost = 40.0,
                description = "Visita guiada por la Plaza de Armas, Catedral y principales monumentos del Centro Histórico.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_museos",
                name = "Tour de Museos de Lima",
                city = "Lima",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Recorrido cultural por museos y espacios históricos de Lima.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_gastronomia",
                name = "Experiencia Gastronómica",
                city = "Lima",
                category = "Experiencias",
                unitCost = 65.0,
                description = "Experiencia gastronómica para conocer platos representativos de la cocina peruana.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_hotel",
                name = "Hotel en Lima",
                city = "Lima",
                category = "Alojamiento",
                unitCost = 80.0,
                description = "Alojamiento turístico por noche en Lima.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_transporte",
                name = "Transporte Turístico Lima",
                city = "Lima",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico dentro de Lima.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "lima_aeropuerto",
                name = "Traslado Aeropuerto - Hotel",
                city = "Lima",
                category = "Transporte",
                unitCost = 25.0,
                description = "Traslado privado desde el aeropuerto hasta el hotel.",
                defaultQuantity = 1
            ),


            // =========================
            // CUSCO
            // =========================

            CatalogService(
                id = "cusco_machu_picchu",
                name = "Machu Picchu",
                city = "Cusco",
                category = "Tours",
                unitCost = 180.0,
                description = "Visita turística a la ciudadela de Machu Picchu.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_valle_sagrado",
                name = "Tour Valle Sagrado",
                city = "Cusco",
                category = "Tours",
                unitCost = 95.0,
                description = "Visita a los principales atractivos turísticos del Valle Sagrado.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_city_tour",
                name = "City Tour Cusco",
                city = "Cusco",
                category = "Tours",
                unitCost = 55.0,
                description = "Recorrido por los principales atractivos históricos de la ciudad del Cusco.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_montana_colores",
                name = "Montaña de Siete Colores",
                city = "Cusco",
                category = "Aventura",
                unitCost = 85.0,
                description = "Excursión a la Montaña de Siete Colores.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_laguna_humantay",
                name = "Laguna Humantay",
                city = "Cusco",
                category = "Aventura",
                unitCost = 80.0,
                description = "Excursión turística a la Laguna Humantay.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_rafting",
                name = "Rafting en Cusco",
                city = "Cusco",
                category = "Aventura",
                unitCost = 75.0,
                description = "Actividad de aventura y rafting en los alrededores del Cusco.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_hotel",
                name = "Hotel en Cusco",
                city = "Cusco",
                category = "Alojamiento",
                unitCost = 90.0,
                description = "Alojamiento turístico por noche en Cusco.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cusco_transporte",
                name = "Transporte Turístico Cusco",
                city = "Cusco",
                category = "Transporte",
                unitCost = 35.0,
                description = "Servicio de transporte turístico para recorridos en Cusco.",
                defaultQuantity = 1
            ),


            // =========================
            // AREQUIPA
            // =========================

            CatalogService(
                id = "arequipa_city_tour",
                name = "City Tour Arequipa",
                city = "Arequipa",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por los principales atractivos turísticos de Arequipa.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_colca",
                name = "Cañón del Colca",
                city = "Arequipa",
                category = "Tours",
                unitCost = 75.0,
                description = "Excursión turística al Cañón del Colca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_monasterio",
                name = "Monasterio de Santa Catalina",
                city = "Arequipa",
                category = "Tours",
                unitCost = 35.0,
                description = "Visita cultural al Monasterio de Santa Catalina.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_campina",
                name = "Tour Campiña Arequipeña",
                city = "Arequipa",
                category = "Tours",
                unitCost = 45.0,
                description = "Recorrido por los principales atractivos de la campiña arequipeña.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_ruta_gastronomica",
                name = "Ruta Gastronómica Arequipeña",
                city = "Arequipa",
                category = "Experiencias",
                unitCost = 60.0,
                description = "Experiencia gastronómica con platos tradicionales de Arequipa.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_aventura",
                name = "Experiencia de Aventura",
                city = "Arequipa",
                category = "Aventura",
                unitCost = 70.0,
                description = "Actividad turística de aventura en los alrededores de Arequipa.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_hotel",
                name = "Hotel en Arequipa",
                city = "Arequipa",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Arequipa.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "arequipa_transporte",
                name = "Transporte Turístico Arequipa",
                city = "Arequipa",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico dentro de Arequipa.",
                defaultQuantity = 1
            ),


            // =========================
            // ICA
            // =========================

            CatalogService(
                id = "ica_huacachina",
                name = "Tour Huacachina",
                city = "Ica",
                category = "Tours",
                unitCost = 50.0,
                description = "Recorrido turístico por el oasis de Huacachina.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_buggies",
                name = "Buggies y Sandboarding",
                city = "Ica",
                category = "Aventura",
                unitCost = 55.0,
                description = "Actividad de aventura en las dunas de Ica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_nazca",
                name = "Sobrevuelo Líneas de Nazca",
                city = "Ica",
                category = "Tours",
                unitCost = 120.0,
                description = "Sobrevuelo turístico de las Líneas de Nazca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_bodega",
                name = "Tour de Bodegas y Viñedos",
                city = "Ica",
                category = "Experiencias",
                unitCost = 60.0,
                description = "Visita turística a bodegas y viñedos tradicionales de Ica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_cata",
                name = "Experiencia de Cata",
                city = "Ica",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Experiencia de degustación de productos tradicionales de Ica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_paseo_desierto",
                name = "Paseo por el Desierto",
                city = "Ica",
                category = "Aventura",
                unitCost = 65.0,
                description = "Recorrido turístico por las dunas y paisajes del desierto de Ica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_hotel",
                name = "Hotel en Ica",
                city = "Ica",
                category = "Alojamiento",
                unitCost = 65.0,
                description = "Alojamiento turístico por noche en Ica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ica_transporte",
                name = "Transporte Turístico Ica",
                city = "Ica",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico para recorridos en Ica.",
                defaultQuantity = 1
            )
        )

        val batch = firestore.batch()

        services.forEach { service ->

            val reference = firestore
                .collection(COLLECTION_SERVICES)
                .document(service.id)

            batch.set(reference, service)
        }

        batch.commit().await()
    }

    companion object {
        const val COLLECTION_SERVICES = "catalog_services"
    }
}