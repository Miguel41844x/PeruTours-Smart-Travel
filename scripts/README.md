# Scripts de mantenimiento

## Reparar relaciones y cargar ejemplos

Estos comandos usan la cuenta que ya inició sesión con Firebase CLI. Instala
las dependencias de `firestore-rules-tests` y `functions` con `npm ci` en sus
respectivas carpetas. No requieren descargar una cuenta de servicio.

Desde la raíz del proyecto:

```powershell
node scripts/repair-data.mjs
node scripts/repair-data.mjs --apply
node scripts/seed-demo.mjs
node scripts/seed-demo.mjs --apply
```

Sin `--apply` solo se genera una vista previa. Antes de escribir se guarda un
respaldo de los documentos leídos y del plan de cambios en `.artifacts/`,
carpeta ignorada por Git. Las actualizaciones usan la versión leída del documento:
si otro integrante lo modifica, el lote afectado se rechaza para no pisar su edición.
Los lotes anteriores pueden haberse aplicado; vuelve a generar la vista previa.

La reparación completa IDs, referencias y campos opcionales compatibles con los
modelos actuales. Conserva importes, estados y snapshots históricos existentes.
Las relaciones de los cuatro paquetes históricos están en `seed/catalog-links.json`;
también corrige el paquete de Máncora que apuntaba a servicios de Piura ciudad.
Los destinos sin contenido editorial suficiente se crean como borradores.

La carga de demostración añade dos clientes, un agente y un gerente, ocho
expedientes en distintos estados, pagos simulados y actividades de itinerario.
Los IDs empiezan por `demo_`, los proveedores se presentan como demostración
y el comprobante dice que no acredita un pago real. Estos pagos **sí participan
en los gráficos**: son ejemplos académicos, no ingresos reales de una agencia.
Las cuentas no reciben correos ni notificaciones; sus credenciales nuevas quedan
únicamente en `.artifacts/demo-accounts.json`. No compartas ese archivo en Git.

Ambos comandos se pueden repetir: la reparación solo actualiza diferencias y
la carga de ejemplos conserva los documentos existentes y sus contraseñas.

## `import-catalog.mjs` — migración del JSON de GitHub a Firestore

La app dejó de consumir `api/perutours-destinations.json` desde
`raw.githubusercontent.com`; ahora el catálogo vive en Firestore. Este script
importa ese JSON histórico (guardado como respaldo en
`scripts/seed/perutours-destinations.json`):

- Crea un **destino** por departamento (`destinations/{destinationId}`) con su
  nombre, resumen y coordenadas.
- Crea un **paquete** por ítem del JSON (`packages/{packageId}`) con precio,
  duración, categoría, imagen y condiciones.
- Ambos quedan en estado `PUBLICADO` para que el cliente los vea.

### Requisitos

- Node.js 18 o superior.
- Cuenta de servicio de Firebase con permiso de escritura en Firestore
  (consola → Configuración del proyecto → Cuentas de servicio →
  *Generar nueva clave privada*).

### Ejecución

```bash
cd scripts
npm install
export GOOGLE_APPLICATION_CREDENTIALS="./serviceAccount.json"   # Windows: set GOOGLE_APPLICATION_CREDENTIALS=...
node import-catalog.mjs --dry-run    # vista previa sin escribir nada
node import-catalog.mjs              # escritura real
```

En **PowerShell** (Windows) el export de credenciales se hace así:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\ruta\completa\serviceAccount.json"
```

### Geocodificación

El script lee la clave de Geoapify de `local.properties` (propiedad
`GEOAPIFY_API_KEY`, la misma que usa la app) o de la variable de entorno
`GEOAPIFY_API_KEY`. Con ella valida cada departamento con la API de
Geoapify y guarda las coordenadas reales más el identificador externo del
lugar. Sin la clave usa coordenadas de respaldo por departamento, que puedes
corregir después desde la pantalla de destinos del administrador.

> La clave de Geoapify nunca se guarda en el repositorio: va en variables de
> entorno o en `local.properties` de la app (`GEOAPIFY_API_KEY`).
