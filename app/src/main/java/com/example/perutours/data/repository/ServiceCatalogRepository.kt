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

            // =========================
            // PIURA
            // =========================

            CatalogService(
                id = "piura_01",
                name = "City Tour Piura",
                city = "Piura",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por la Plaza de Armas, la catedral y el centro histórico de Piura.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_02",
                name = "Excursión a Catacaos",
                city = "Piura",
                category = "Tours",
                unitCost = 45.0,
                description = "Visita al pueblo artesanal de Catacaos y sus talleres tradicionales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_03",
                name = "Tour a las Playas de Colán",
                city = "Piura",
                category = "Tours",
                unitCost = 60.0,
                description = "Excursión turística a la playa de Colán.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_04",
                name = "Ruta Cultural de Narihualá",
                city = "Piura",
                category = "Tours",
                unitCost = 45.0,
                description = "Visita al complejo arqueológico y museo de Narihualá.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_05",
                name = "Paseo en Bicicleta por la Costa",
                city = "Piura",
                category = "Aventura",
                unitCost = 50.0,
                description = "Recorrido recreativo en bicicleta por rutas costeras seleccionadas.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_06",
                name = "Ruta Gastronómica Piurana",
                city = "Piura",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Experiencia culinaria con platos típicos de Piura.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_07",
                name = "Hotel en Piura",
                city = "Piura",
                category = "Alojamiento",
                unitCost = 65.0,
                description = "Alojamiento turístico por noche en Piura.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "piura_08",
                name = "Transporte Turístico Piura",
                city = "Piura",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de traslados turísticos en Piura y alrededores.",
                defaultQuantity = 1
            ),

            // =========================
            // TUMBES
            // =========================

            CatalogService(
                id = "tumbes_01",
                name = "City Tour Tumbes",
                city = "Tumbes",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por los principales atractivos urbanos e históricos de Tumbes.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_02",
                name = "Santuario Nacional Los Manglares",
                city = "Tumbes",
                category = "Tours",
                unitCost = 65.0,
                description = "Excursión guiada por los manglares y su entorno natural.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_03",
                name = "Tour a Punta Sal",
                city = "Tumbes",
                category = "Tours",
                unitCost = 55.0,
                description = "Visita turística a la playa de Punta Sal.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_04",
                name = "Tour a Puerto Pizarro",
                city = "Tumbes",
                category = "Tours",
                unitCost = 50.0,
                description = "Paseo turístico por Puerto Pizarro y sus paisajes costeros.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_05",
                name = "Paseo en Kayak por Manglares",
                city = "Tumbes",
                category = "Aventura",
                unitCost = 60.0,
                description = "Actividad en kayak por zonas permitidas de los manglares.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_06",
                name = "Ruta Gastronómica Tumbesina",
                city = "Tumbes",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Degustación de platos tradicionales y productos marinos de Tumbes.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_07",
                name = "Hotel en Tumbes",
                city = "Tumbes",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Tumbes.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tumbes_08",
                name = "Transporte Turístico Tumbes",
                city = "Tumbes",
                category = "Transporte",
                unitCost = 35.0,
                description = "Traslados turísticos dentro de Tumbes y sus playas cercanas.",
                defaultQuantity = 1
            ),

            // =========================
            // CAJAMARCA
            // =========================

            CatalogService(
                id = "cajamarca_01",
                name = "City Tour Cajamarca",
                city = "Cajamarca",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por la Plaza de Armas, la Catedral y el centro histórico.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_02",
                name = "Ventanillas de Otuzco",
                city = "Cajamarca",
                category = "Tours",
                unitCost = 45.0,
                description = "Visita guiada al conjunto funerario de Ventanillas de Otuzco.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_03",
                name = "Complejo Arqueológico Cumbemayo",
                city = "Cajamarca",
                category = "Tours",
                unitCost = 60.0,
                description = "Excursión por el bosque de piedras y los canales prehispánicos de Cumbemayo.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_04",
                name = "Cuarto del Rescate",
                city = "Cajamarca",
                category = "Tours",
                unitCost = 30.0,
                description = "Visita cultural al histórico Cuarto del Rescate.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_05",
                name = "Ruta de Queserías Cajamarquinas",
                city = "Cajamarca",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Visita a productores locales para conocer la elaboración de quesos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_06",
                name = "Ruta Gastronómica Cajamarquina",
                city = "Cajamarca",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Experiencia culinaria con especialidades tradicionales de Cajamarca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_07",
                name = "Hotel en Cajamarca",
                city = "Cajamarca",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Cajamarca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "cajamarca_08",
                name = "Transporte Turístico Cajamarca",
                city = "Cajamarca",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico para recorridos locales.",
                defaultQuantity = 1
            ),

            // =========================
            // AYACUCHO
            // =========================

            CatalogService(
                id = "ayacucho_01",
                name = "City Tour Ayacucho",
                city = "Ayacucho",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por plazas, iglesias y casonas del centro histórico de Ayacucho.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_02",
                name = "Complejo Arqueológico Wari",
                city = "Ayacucho",
                category = "Tours",
                unitCost = 50.0,
                description = "Visita guiada al complejo arqueológico de la cultura Wari.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_03",
                name = "Pampa de Ayacucho",
                city = "Ayacucho",
                category = "Tours",
                unitCost = 45.0,
                description = "Excursión al Santuario Histórico de la Pampa de Ayacucho.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_04",
                name = "Cascadas de Millpu",
                city = "Ayacucho",
                category = "Tours",
                unitCost = 85.0,
                description = "Excursión a las pozas y cascadas de Millpu.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_05",
                name = "Taller de Retablos Ayacuchanos",
                city = "Ayacucho",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Actividad artesanal para conocer la elaboración de retablos ayacuchanos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_06",
                name = "Ruta Gastronómica Ayacuchana",
                city = "Ayacucho",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Degustación de platos tradicionales de Ayacucho.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_07",
                name = "Hotel en Ayacucho",
                city = "Ayacucho",
                category = "Alojamiento",
                unitCost = 65.0,
                description = "Alojamiento turístico por noche en Ayacucho.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "ayacucho_08",
                name = "Transporte Turístico Ayacucho",
                city = "Ayacucho",
                category = "Transporte",
                unitCost = 30.0,
                description = "Traslados turísticos dentro de Ayacucho y sus alrededores.",
                defaultQuantity = 1
            ),

            // =========================
            // HUANCAVELICA
            // =========================

            CatalogService(
                id = "huancavelica_01",
                name = "City Tour Huancavelica",
                city = "Huancavelica",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por la Plaza de Armas, iglesias y principales espacios históricos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_02",
                name = "Complejo Termal de San Cristóbal",
                city = "Huancavelica",
                category = "Tours",
                unitCost = 40.0,
                description = "Visita a los baños termales de San Cristóbal.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_03",
                name = "Ruta de las Iglesias Coloniales",
                city = "Huancavelica",
                category = "Tours",
                unitCost = 45.0,
                description = "Recorrido cultural por iglesias históricas de Huancavelica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_04",
                name = "Caminata por Paisajes Altoandinos",
                city = "Huancavelica",
                category = "Aventura",
                unitCost = 55.0,
                description = "Caminata guiada por paisajes altoandinos de la región.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_05",
                name = "Ruta de Artesanía Huancavelicana",
                city = "Huancavelica",
                category = "Experiencias",
                unitCost = 40.0,
                description = "Encuentro con artesanos locales y sus técnicas tradicionales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_06",
                name = "Ruta Gastronómica Huancavelicana",
                city = "Huancavelica",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Experiencia culinaria con platos típicos de Huancavelica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_07",
                name = "Hotel en Huancavelica",
                city = "Huancavelica",
                category = "Alojamiento",
                unitCost = 60.0,
                description = "Alojamiento turístico por noche en Huancavelica.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huancavelica_08",
                name = "Transporte Turístico Huancavelica",
                city = "Huancavelica",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico para recorridos locales.",
                defaultQuantity = 1
            ),

            // =========================
            // HUARAZ
            // =========================

            CatalogService(
                id = "huaraz_01",
                name = "City Tour Huaraz",
                city = "Huaraz",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por la Plaza de Armas, museos y atractivos urbanos de Huaraz.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_02",
                name = "Laguna 69",
                city = "Huaraz",
                category = "Aventura",
                unitCost = 95.0,
                description = "Excursión de trekking hacia la famosa Laguna 69, sujeta a condiciones climáticas.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_03",
                name = "Laguna de Llanganuco",
                city = "Huaraz",
                category = "Tours",
                unitCost = 65.0,
                description = "Visita a las lagunas de Llanganuco dentro del Parque Nacional Huascarán.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_04",
                name = "Nevado Pastoruri",
                city = "Huaraz",
                category = "Aventura",
                unitCost = 75.0,
                description = "Excursión al sector turístico del nevado Pastoruri.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_05",
                name = "Complejo Arqueológico Chavín",
                city = "Huaraz",
                category = "Tours",
                unitCost = 85.0,
                description = "Visita guiada al sitio arqueológico de Chavín de Huántar.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_06",
                name = "Trekking por la Cordillera Blanca",
                city = "Huaraz",
                category = "Aventura",
                unitCost = 100.0,
                description = "Caminata guiada por rutas seleccionadas de la Cordillera Blanca.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_07",
                name = "Hotel en Huaraz",
                city = "Huaraz",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Huaraz.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "huaraz_08",
                name = "Transporte Turístico Huaraz",
                city = "Huaraz",
                category = "Transporte",
                unitCost = 35.0,
                description = "Traslados turísticos en Huaraz y puntos de inicio de excursiones.",
                defaultQuantity = 1
            ),

            // =========================
            // TARAPOTO
            // =========================

            CatalogService(
                id = "tarapoto_01",
                name = "City Tour Tarapoto",
                city = "Tarapoto",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por los principales atractivos urbanos de Tarapoto.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_02",
                name = "Laguna Azul de Sauce",
                city = "Tarapoto",
                category = "Tours",
                unitCost = 75.0,
                description = "Excursión a la Laguna Azul y sus paisajes naturales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_03",
                name = "Catarata de Ahuashiyacu",
                city = "Tarapoto",
                category = "Tours",
                unitCost = 45.0,
                description = "Visita a la catarata de Ahuashiyacu y su entorno natural.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_04",
                name = "Canotaje en el Río Mayo",
                city = "Tarapoto",
                category = "Aventura",
                unitCost = 65.0,
                description = "Actividad recreativa de canotaje en sectores autorizados del río Mayo.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_05",
                name = "Lamas y Castillo de Lamas",
                city = "Tarapoto",
                category = "Tours",
                unitCost = 50.0,
                description = "Recorrido cultural por Lamas y sus atractivos locales.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_06",
                name = "Ruta del Cacao y Chocolate",
                city = "Tarapoto",
                category = "Experiencias",
                unitCost = 55.0,
                description = "Visita a productores locales para conocer el proceso del cacao.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_07",
                name = "Hotel en Tarapoto",
                city = "Tarapoto",
                category = "Alojamiento",
                unitCost = 70.0,
                description = "Alojamiento turístico por noche en Tarapoto.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tarapoto_08",
                name = "Transporte Turístico Tarapoto",
                city = "Tarapoto",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de transporte turístico dentro de Tarapoto y alrededores.",
                defaultQuantity = 1
            ),

            // =========================
            // IQUITOS
            // =========================

            CatalogService(
                id = "iquitos_01",
                name = "City Tour Iquitos",
                city = "Iquitos",
                category = "Tours",
                unitCost = 40.0,
                description = "Recorrido por la Plaza de Armas, el malecón y la arquitectura histórica de Iquitos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_02",
                name = "Excursión por el Río Amazonas",
                city = "Iquitos",
                category = "Tours",
                unitCost = 90.0,
                description = "Paseo fluvial guiado por sectores turísticos del río Amazonas.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_03",
                name = "Reserva Nacional Pacaya Samiria",
                city = "Iquitos",
                category = "Tours",
                unitCost = 150.0,
                description = "Excursión de naturaleza con operadores autorizados y logística local.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_04",
                name = "Visita a Comunidad Amazónica",
                city = "Iquitos",
                category = "Experiencias",
                unitCost = 75.0,
                description = "Experiencia cultural organizada con una comunidad local participante.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_05",
                name = "Observación de Fauna Amazónica",
                city = "Iquitos",
                category = "Aventura",
                unitCost = 85.0,
                description = "Actividad de observación de fauna en su hábitat natural con guía.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_06",
                name = "Isla de los Monos",
                city = "Iquitos",
                category = "Tours",
                unitCost = 65.0,
                description = "Visita a un centro de conservación de primates y educación ambiental.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_07",
                name = "Hotel en Iquitos",
                city = "Iquitos",
                category = "Alojamiento",
                unitCost = 80.0,
                description = "Alojamiento turístico por noche en Iquitos.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "iquitos_08",
                name = "Transporte Turístico Iquitos",
                city = "Iquitos",
                category = "Transporte",
                unitCost = 35.0,
                description = "Traslados turísticos terrestres y coordinación de transporte fluvial local.",
                defaultQuantity = 1
            ),

            // =========================
            // PUERTO MALDONADO
            // =========================

            CatalogService(
                id = "puerto_maldonado_01",
                name = "City Tour Puerto Maldonado",
                city = "Puerto Maldonado",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por los principales atractivos urbanos de Puerto Maldonado.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_02",
                name = "Lago Sandoval",
                city = "Puerto Maldonado",
                category = "Tours",
                unitCost = 85.0,
                description = "Excursión guiada al lago Sandoval en la Reserva Nacional Tambopata.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_03",
                name = "Reserva Nacional Tambopata",
                city = "Puerto Maldonado",
                category = "Tours",
                unitCost = 120.0,
                description = "Experiencia de naturaleza en sectores autorizados de Tambopata.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_04",
                name = "Caminata por la Selva Amazónica",
                city = "Puerto Maldonado",
                category = "Aventura",
                unitCost = 70.0,
                description = "Caminata interpretativa por senderos naturales con guía local.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_05",
                name = "Visita a Centro de Rescate de Fauna",
                city = "Puerto Maldonado",
                category = "Experiencias",
                unitCost = 55.0,
                description = "Visita educativa a un centro autorizado de rescate de fauna silvestre.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_06",
                name = "Paseo por el Río Madre de Dios",
                city = "Puerto Maldonado",
                category = "Tours",
                unitCost = 60.0,
                description = "Recorrido fluvial turístico por el río Madre de Dios.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_07",
                name = "Hotel en Puerto Maldonado",
                city = "Puerto Maldonado",
                category = "Alojamiento",
                unitCost = 75.0,
                description = "Alojamiento turístico por noche en Puerto Maldonado.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "puerto_maldonado_08",
                name = "Transporte Turístico Puerto Maldonado",
                city = "Puerto Maldonado",
                category = "Transporte",
                unitCost = 35.0,
                description = "Traslados turísticos dentro de la ciudad y puntos de excursión.",
                defaultQuantity = 1
            ),

            // =========================
            // TACNA
            // =========================

            CatalogService(
                id = "tacna_01",
                name = "City Tour Tacna",
                city = "Tacna",
                category = "Tours",
                unitCost = 35.0,
                description = "Recorrido por el centro histórico y los principales monumentos de Tacna.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_02",
                name = "Complejo Monumental Alto de la Alianza",
                city = "Tacna",
                category = "Tours",
                unitCost = 40.0,
                description = "Visita cultural al complejo monumental y museo de sitio.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_03",
                name = "Ruta del Pisco Tacneño",
                city = "Tacna",
                category = "Tours",
                unitCost = 55.0,
                description = "Visita a productores locales para conocer la elaboración de pisco.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_04",
                name = "Paseo a Boca del Río",
                city = "Tacna",
                category = "Tours",
                unitCost = 45.0,
                description = "Excursión al balneario Boca del Río.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_05",
                name = "Ruta Gastronómica Tacneña",
                city = "Tacna",
                category = "Experiencias",
                unitCost = 45.0,
                description = "Experiencia culinaria con platos típicos de Tacna.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_06",
                name = "Circuito de Petroglifos de Miculla",
                city = "Tacna",
                category = "Experiencias",
                unitCost = 50.0,
                description = "Visita guiada al sitio arqueológico de Miculla.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_07",
                name = "Hotel en Tacna",
                city = "Tacna",
                category = "Alojamiento",
                unitCost = 65.0,
                description = "Alojamiento turístico por noche en Tacna.",
                defaultQuantity = 1
            ),

            CatalogService(
                id = "tacna_08",
                name = "Transporte Turístico Tacna",
                city = "Tacna",
                category = "Transporte",
                unitCost = 30.0,
                description = "Servicio de traslados turísticos dentro de Tacna y alrededores.",
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