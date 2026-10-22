# Scripts de mantenimiento

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
