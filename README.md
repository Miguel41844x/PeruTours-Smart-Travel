# PeruTours — Smart Travel 🇵🇪

App Android (Kotlin + Jetpack Compose) para cotizar y organizar viajes por el Perú.
Arquitectura **MVVM** con Koin, corrutinas/Flow, Firebase (Auth, Firestore, Storage,
FCM), Retrofit para APIs externas y Room para lectura offline.

## HU10 — Recomendaciones turísticas

La pantalla principal incluye la sección **"Recomendado para ti"**, que sugiere
destinos personalizados usando:

- **IA: Google Gemini 3.8 Flash** (tier gratuito de Google AI Studio) llamado por
  **Retrofit + corrutinas**, que devuelve un JSON con los destinos rankeados y una
  razón personalizada en español.
- **Recomendador local de respaldo** (filtrado de contenido por reglas): si no hay
  clave de Gemini, no hay red o la IA falla, la sección sigue funcionando.
- **Firebase**: guarda las preferencias del usuario (tipo de viaje, presupuesto,
  duración y categorías de interés) y la última recomendación en
  `users/{uid}/recommendations/last` como caché de 6 horas.
- **API externa de información turística**: resúmenes de **Wikipedia** (Retrofit +
  corrutinas) que enriquecen cada tarjeta.

### Configuración de las claves de API

Las claves NUNCA se commitean: se leen de `local.properties` (ya ignorado por git)
y se inyectan en `BuildConfig`.

1. Obtén una clave **gratuita** en [Google AI Studio](https://aistudio.google.com)
   (no requiere tarjeta de crédito).
2. Abre `local.properties` en la raíz del proyecto y agrega:

   ```properties
   GEMINI_API_KEY=tu_clave_de_gemini_aqui
   GEOAPIFY_API_KEY=tu_clave_de_geoapify_aqui
   ```

3. Sincroniza Gradle (Android Studio lo hace automáticamente).

> Sin `GEMINI_API_KEY` la app funciona igual: la sección usa el recomendador
> local por reglas en lugar de la IA.

### Preferencias del usuario (HU10)

En **Perfil → Preferencias de viaje** el cliente registra:

| Campo | Valores permitidos |
|---|---|
| Tipo de viaje | Aventura, Cultura, Relax, Gastronomía, Naturaleza, Familiar |
| Presupuesto | Económico, Medio, Premium |
| Duración | Fin de semana, 3-5 días, 1 semana, 2+ semanas |
| Categorías de interés | Catálogo de Firestore (`preferences`) |

Estos valores están acotados también en `firestore.rules` (HU10).

## Ejecutar las pruebas

```bash
# Pruebas unitarias (validador, recomendador local, ViewModels, etc.)
gradlew testDebugUnitTest

# Pruebas de reglas de Firestore (emulador)
cd firestore-rules-tests
npm install
npm test
```

## Estructura (HU10)

```
data/model/Recommendation.kt            → modelos Recommendation y TravelPreferences
data/model/TravelProfileOptions.kt      → valores permitidos de las preferencias
data/remote/gemini/                     → cliente Retrofit de Gemini
data/remote/wikipedia/                  → cliente Retrofit de Wikipedia
data/repository/RecommendationRepository.kt → orquestador (IA + fallback + caché + wiki)
domain/recommendation/                  → prompt, parser, recomendador local
ui/screens/recommendation/              → ViewModel MVVM + estados sellados
ui/screens/home/HomeScreen.kt           → sección "Recomendado para ti"
```
