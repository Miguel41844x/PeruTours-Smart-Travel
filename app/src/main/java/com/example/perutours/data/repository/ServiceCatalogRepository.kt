package com.example.perutours.data.repository

import com.example.perutours.data.model.CatalogService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ServiceCatalogRepository(
    private val firestore: FirebaseFirestore
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
            ),


            // =========================
            // JUNÍN
            // =========================

            CatalogService(
                id = "junin_city_tour",
                name = "City Tour Huancayo y Junín",
                city = "Junín",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por la Plaza Constitución, la Catedral y principales atractivos de la ciudad.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_huaytapallana",
                name = "Excursión al Nevado Huaytapallana",
                city = "Junín",
                category = "Aventura",
                unitCost = 85.0,
                description = "Caminata de alta montaña y vistas panorámicas del majestuoso nevado Huaytapallana.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_wariwillka",
                name = "Complejo Arqueológico Wariwillka",
                city = "Junín",
                category = "Tours",
                unitCost = 35.0,
                description = "Visita guiada al santuario preincaico y museo de sitio Wariwillka.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_cochas",
                name = "Tour Artesanal Cochas Chico y Grande",
                city = "Junín",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Conoce el arte de los mates burilados y la labor de los artesanos locales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_shucto",
                name = "Aventura en el Cañón de Shucto",
                city = "Junín",
                category = "Aventura",
                unitCost = 65.0,
                description = "Exploración y trekking por las formaciones rocosas del impresionante cañón de Shucto.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_gastronomia",
                name = "Ruta Gastronómica Huancaína",
                city = "Junín",
                category = "Experiencias",
                unitCost = 55.0,
                description = "Experiencia culinaria para degustar la auténtica pachamanca, papa a la huancaína y platos típicos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_hotel",
                name = "Hotel en Junín / Huancayo",
                city = "Junín",
                category = "Alojamiento",
                unitCost = 75.0,
                description = "Alojamiento turístico por noche en la región Junín.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "junin_transporte",
                name = "Transporte Turístico Junín",
                city = "Junín",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico para traslados y excursiones en Junín.",
                defaultQuantity = 1
            ),

            // =========================
            // PUNO
            // =========================

            CatalogService(
                id = "puno_01",
                name = "City Tour Puno",
                city = "Puno",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por la Plaza de Armas, la Catedral y los principales atractivos de Puno.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_02",
                name = "Islas Flotantes de los Uros",
                city = "Puno",
                category = "Tours",
                unitCost = 55.0,
                description = "Visita guiada a las tradicionales islas flotantes del lago Titicaca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_03",
                name = "Isla Taquile",
                city = "Puno",
                category = "Tours",
                unitCost = 85.0,
                description = "Excursión a la isla Taquile para conocer sus paisajes y tradiciones textiles.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_04",
                name = "Experiencia Cultural Aymara",
                city = "Puno",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Actividad cultural para conocer costumbres y tradiciones locales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_05",
                name = "Kayak en el Lago Titicaca",
                city = "Puno",
                category = "Aventura",
                unitCost = 70.0,
                description = "Paseo en kayak por sectores autorizados del lago Titicaca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_06",
                name = "Ruta Gastronómica Puneña",
                city = "Puno",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Degustación de platos tradicionales de la región de Puno.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_07",
                name = "Hotel en Puno",
                city = "Puno",
                category = "Alojamiento",
                unitCost = 75.0,
                description = "Alojamiento turístico por noche en Puno.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puno_08",
                name = "Transporte Turístico Puno",
                city = "Puno",
                category = "Transporte",
                unitCost = 30.0,
                description = "Traslados turísticos dentro de Puno y sus alrededores.",
                defaultQuantity = 1
            ),

            // =========================
            // TRUJILLO
            // =========================

            CatalogService(
                id = "trujillo_01",
                name = "City Tour Trujillo",
                city = "Trujillo",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por el centro histórico y los principales atractivos de Trujillo.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_02",
                name = "Complejo Arqueológico Chan Chan",
                city = "Trujillo",
                category = "Tours",
                unitCost = 55.0,
                description = "Visita guiada a la antigua ciudad de adobe de Chan Chan.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_03",
                name = "Huacas del Sol y de la Luna",
                city = "Trujillo",
                category = "Tours",
                unitCost = 60.0,
                description = "Recorrido arqueológico por las huacas y su museo de sitio.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_04",
                name = "Tour a Huanchaco",
                city = "Trujillo",
                category = "Tours",
                unitCost = 35.0,
                description = "Visita al balneario de Huanchaco y sus tradicionales caballitos de totora.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_05",
                name = "Ruta Gastronómica Trujillana",
                city = "Trujillo",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Experiencia culinaria con platos tradicionales de La Libertad.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_06",
                name = "Taller de Artesanía Moche",
                city = "Trujillo",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Actividad cultural para conocer técnicas artesanales inspiradas en la cultura Moche.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_07",
                name = "Hotel en Trujillo",
                city = "Trujillo",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Trujillo.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "trujillo_08",
                name = "Transporte Turístico Trujillo",
                city = "Trujillo",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico dentro de Trujillo y sus alrededores.",
                defaultQuantity = 1
            ),

            // =========================
            // CHICLAYO
            // =========================

            CatalogService(
                id = "chiclayo_01",
                name = "City Tour Chiclayo",
                city = "Chiclayo",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por el centro de Chiclayo y sus principales atractivos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_02",
                name = "Museo Tumbas Reales de Sipán",
                city = "Chiclayo",
                category = "Tours",
                unitCost = 55.0,
                description = "Visita cultural al museo dedicado al Señor de Sipán.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_03",
                name = "Complejo Arqueológico Túcume",
                city = "Chiclayo",
                category = "Tours",
                unitCost = 60.0,
                description = "Excursión por las pirámides y el museo de sitio de Túcume.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_04",
                name = "Santuario Histórico Bosque de Pómac",
                city = "Chiclayo",
                category = "Tours",
                unitCost = 65.0,
                description = "Recorrido por el bosque seco y sus atractivos arqueológicos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_05",
                name = "Ruta Gastronómica Lambayecana",
                city = "Chiclayo",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Degustación de platos típicos de Lambayeque.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_06",
                name = "Taller de Cerámica Tradicional",
                city = "Chiclayo",
                category = "Experiencias",
                unitCost = 40.0,
                description = "Actividad para conocer la cerámica y artesanía tradicional de la región.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_07",
                name = "Hotel en Chiclayo",
                city = "Chiclayo",
                category = "Alojamiento",
                unitCost = 65.0,
                description = "Alojamiento turístico por noche en Chiclayo.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "chiclayo_08",
                name = "Transporte Turístico Chiclayo",
                city = "Chiclayo",
                category = "Transporte",
                unitCost = 30.0,
                description = "Traslados turísticos dentro de Chiclayo y zonas cercanas.",
                defaultQuantity = 1
            ),
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