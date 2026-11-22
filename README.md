# PeruTours — Smart Travel 🇵🇪

App Android (Kotlin + Jetpack Compose) para cotizar y organizar viajes por el Perú.
Arquitectura **MVVM** con Koin, corrutinas/Flow, Firebase (Auth, Firestore, Storage,
FCM), Retrofit para APIs externas y Room para lectura offline.

## Procesos de negocio

1. **Gestión de la oferta:** el administrador mantiene destinos, proveedores,
   servicios y paquetes. El paquete referencia servicios del mismo destino;
   publicar o desactivar controla qué se ofrece al cliente.
2. **Venta del viaje:** el cliente solicita un paquete o un viaje personalizado,
   elige actividades opcionales y recibe una propuesta del agente. Puede observarla,
   aceptarla o cancelarla. La aceptación crea la reserva; cada comprobante es un
   intento de pago independiente que el agente asignado revisa.
3. **Operación y atención del viaje:** los servicios de la reserva pagada alimentan
   el itinerario. El agente fija sus horarios y atiende incidencias; el cliente
   consulta el viaje, prepara su checklist y recibe recordatorios.

El destino identifica el lugar; el servicio identifica una prestación de un
proveedor; el paquete agrupa servicios; la cotización fija condiciones y cantidades;
la reserva conserva lo aceptado. Los snapshots evitan que editar el catálogo cambie
una venta anterior. Pedir una actividad expresa un interés: el agente confirma
su disponibilidad y precio antes de que el cliente acepte.

## Requisitos del curso y trazabilidad

| Requisito | Implementación |
|---|---|
| Autenticación | Firebase Auth, verificación de correo, recuperación de contraseña; `AuthRepository` y roles en `UserRole`. |
| Tres procesos | Gestión de oferta, venta de viajes y operación posterior a la venta, descritos arriba. |
| Firebase | Auth, Firestore, Storage, FCM, Cloud Functions y Firebase AI Logic. Reglas de acceso por propietario y rol. |
| Material Design | Compose Material 3, tema compartido, estados de carga/error/vacío y colores para tema claro y oscuro. |
| MVVM e inyección | Screens y UiState en `ui/screens`, ViewModels, repositorios en `data/repository`, funciones de dominio en `domain`; Koin en `di/AppModule.kt`. |
| Corrutinas y Retrofit | Geoapify para lugares y puntos de interés; Wikipedia para información; respaldo REST de Gemini. Funciones `suspend`, Flow y `await()` para Firebase. |
| WorkManager | `UploadPaymentWorker`, `TripSyncWorker` y `TripReminderWorker`; reintentos, sincronización del checklist y recordatorios únicos. |
| SQLite | Room: `AppDatabase` v9, DAOs y entidades de catálogo, lugares, viajes, checklist, incidencias y cachés administrativos. Migraciones conservan las ediciones pendientes. |
| Tres dashboards | Administrador: Vico y Tehras; gerente: MPAndroidChart con conversión, ingresos por moneda y demanda; agente: cartera de solicitudes y pagos pendientes. |
| Tres recursos móviles | Ubicación/GPS, cámara con CameraX para comprobantes y audio con TextToSpeech para leer el itinerario. |
| Tres funciones de IA | Recomendaciones con Gemini, asistente contextual con Gemini y OCR de comprobantes con ML Kit. El respaldo por reglas se distingue de una respuesta generada con IA. |

Los filtros de agente permiten buscar solicitudes, comprobantes y viajes, además
de ordenar y mostrar registros por bloques. Actualmente las solicitudes y los pagos
se leen en conjunto y se filtran en memoria; los bloques de la interfaz no equivalen
a paginación del servidor para una agencia con miles de documentos.

## Datos y configuración local

El catálogo que consume la app vive en Firestore. El JSON histórico de `scripts/seed`
se conserva como herramienta de importación, no como API externa de la aplicación.
Los datos de demostración están identificados como simulados y sus pagos participan
en los gráficos. Consulta `scripts/README.md` para reparación, respaldos y carga de ejemplos.

Las cotizaciones nuevas usan los costos de servicios en soles. Las reservas y los
pagos conservan su moneda; los dashboards no suman PEN y USD como si fueran iguales.
El precio de un paquete es referencial hasta que el cliente acepta la propuesta.

En otro equipo configura `sdk.dir`, `GEOAPIFY_API_KEY` y, si se necesita el respaldo
REST, `GEMINI_API_KEY` en `local.properties`. Este archivo, los respaldos y las
credenciales de demostración quedan fuera de Git. Para Firebase AI Logic también
debe estar habilitado el proveedor de Gemini en el proyecto Firebase.

Jetnews es una app de noticias que Google usa para enseñar a integrar Gemini;
la integración se realiza con Firebase AI Logic. Referencia:
[codelab oficial de Android](https://developer.android.com/codelabs/gemini-summarize).

## HU10 — Recomendaciones turísticas

La pantalla principal incluye la sección **"Recomendado para ti"**, que sugiere
destinos personalizados usando:

- **IA: Google Gemini**, mediante Firebase AI Logic y respaldo REST con
  **Retrofit + corrutinas**, devuelve destinos ordenados y una razón personalizada.
  El modelo compartido está en `GeminiClient.MODEL`.
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

1. Obtén una clave en [Google AI Studio](https://aistudio.google.com).
2. Abre `local.properties` en la raíz del proyecto y agrega:

   ```properties
   GEMINI_API_KEY=tu_clave_de_gemini_aqui
   GEOAPIFY_API_KEY=tu_clave_de_geoapify_aqui
   ```

3. Sincroniza Gradle (Android Studio lo hace automáticamente).

> Sin clave REST todavía se puede usar el SDK de Firebase AI Logic si está
> configurado. Si la IA no responde, el recomendador local mantiene las sugerencias
> y el chat informa que está usando un respaldo basado en datos existentes.

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

# Compilar app y APK de pruebas de Android
gradlew assembleDebug assembleDebugAndroidTest

# Análisis estático (requiere descargar las dependencias de lint la primera vez)
gradlew lintDebug --no-configuration-cache

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


## HU13 — Destinos y paquetes favoritos

El corazón del catálogo y del detalle permite guardar o quitar destinos y paquetes.
La lista Mis favoritos conserva título, ubicación, descripción, precio y duración
cuando corresponde, permite buscar y filtrar, y se consulta sin red. Las imágenes
pueden depender de la caché de Coil. Room separa cada selección por usuario, tipo e ID;
el mismo ID de un destino y un paquete no se sobrescribe. La migración 8→9 conserva
los registros anteriores y el checklist pendiente de sincronizar. Los precios
guardados son informativos: una nueva solicitud valida el catálogo vigente.

## HU14 — Notificaciones y recordatorios

Los avisos FCM existentes abren la cotización, reserva o incidencia correspondiente.
El acceso espera a que termine la autenticación y la resolución del rol.
Los recordatorios de salidas pagadas se programan con WorkManager unas 24 horas antes;
una sincronización periódica con red recoge cambios incluso con la app cerrada.
Se comprueba la sesión, el propietario, el estado pagado y la fecha vigente antes
de mostrar el aviso. El registro local de entregas evita repetir la misma salida.
Perfil muestra el estado del permiso y permite activar o gestionar las notificaciones.
WorkManager respeta las restricciones de batería de Android: el aviso no es una alarma exacta.

## Revisión automática de comprobantes

ML Kit lee importe, moneda, operación, fecha y hora; el validador compara lo obtenido
con la reserva. Bloquea incoherencias conocidas como importe distinto, fecha futura
(incluido un voucher de 2050) o anterior a la reserva. Las horas se interpretan en
la zona de Perú, con cinco minutos de tolerancia de reloj. Un dato ilegible queda
para revisión y nunca se completa con un dato inventado. En registros antiguos
sin fecha de creación se informa que falta esa comprobación.

El OCR no consulta el saldo bancario ni prueba la autenticidad de una imagen.
El agente confirma el abono en la cuenta y solo puede aprobar el intento vigente,
vinculado a su reserva y sin inconsistencias detectadas. Una lectura incompleta
requiere esa comprobación manual; los rechazos admiten entre 10 y 500 caracteres.
