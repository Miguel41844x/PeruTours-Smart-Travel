/**
 * Migra el catálogo histórico (scripts/seed/perutours-destinations.json) a
 * Firestore. Ese JSON era el que la app consumía desde GitHub.
 *
 * Qué hace:
 *  1. Lee el JSON que la app consumía desde GitHub.
 *  2. Por cada ítem crea/actualiza un DESTINO (el lugar, con coordenadas) y
 *     un PAQUETE (la oferta vendible: precio, duración, imagen).
 *  3. Fija el estado PUBLICADO para que el cliente lo vea de inmediato.
 *
 * Uso:
 *   export GOOGLE_APPLICATION_CREDENTIALS="./serviceAccount.json"
 *   node scripts/import-catalog.mjs [--dry-run]
 *
 * Requiere Node.js 18+ y `npm install` dentro de scripts/.
 */

import { readFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { initializeApplicationDefault, getApps } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url));
const JSON_PATH = resolve(SCRIPT_DIR, "seed", "perutours-destinations.json");
const DRY_RUN = process.argv.includes("--dry-run");

/**
 * Coordenadas de respaldo por departamento. Si defines GEOAPIFY_API_KEY el
 * script valida el lugar con la API externa y usa las coordenadas reales.
 */
const FALLBACK_COORDINATES = {
  cusco: { latitude: -13.53195, longitude: -71.96746, externalPlaceId: "fallback-cusco" },
  ica: { latitude: -14.06709, longitude: -75.72611, externalPlaceId: "fallback-ica" },
  arequipa: { latitude: -16.40905, longitude: -71.53745, externalPlaceId: "fallback-arequipa" },
  puno: { latitude: -15.84022, longitude: -70.02188, externalPlaceId: "fallback-puno" }
};

// Combining diacritical marks (U+0300 - U+036F) sin depender de escapes.
function removeDiacritics(text) {
  return text
    .normalize("NFD")
    .split("")
    .filter((char) => {
      const code = char.charCodeAt(0);
      return code < 0x0300 || code > 0x036f;
    })
    .join("");
}

function parsePrice(raw) {
  const digits = String(raw).replace(/[^0-9.]/g, "");
  return Number.parseFloat(digits) || 0;
}

function parseDurationDays(raw) {
  const firstNumber = String(raw).match(/\d+/);
  return firstNumber ? Number.parseInt(firstNumber[0], 10) : 1;
}

function slugify(text) {
  return removeDiacritics(String(text).toLowerCase())
    .replace(/[^a-z0-9]+/g, "_")
    .replace(/^_+|_+$/g, "")
    .slice(0, 60);
}

async function geocodeWithGeoapify(name, department) {
  const apiKey = process.env.GEOAPIFY_API_KEY;
  if (!apiKey) return null;

  const query = encodeURIComponent(`${name}, ${department}, Perú`);
  const url = `https://api.geoapify.com/v1/geocoding/search?text=${query}&lang=es&limit=1&apiKey=${apiKey}`;
  const response = await fetch(url);
  if (!response.ok) return null;

  const payload = await response.json();
  const feature = payload.features?.[0];
  if (!feature) return null;

  const [longitude, latitude] = feature.geometry.coordinates;
  return {
    latitude,
    longitude,
    externalPlaceId:
      feature.properties?.place_id ?? feature.properties?.datasource?.raw?.osm_id ?? ""
  };
}

function fallbackCoordinates(department) {
  const key = slugify(department).split("_")[0];
  return (
    FALLBACK_COORDINATES[key] ?? {
      latitude: -12.0464,
      longitude: -77.0428,
      externalPlaceId: "fallback-lima"
    }
  );
}

async function main() {
  if (getApps().length === 0) {
    initializeApplicationDefault();
  }
  const db = getFirestore();

  const rawCatalog = readFileSync(JSON_PATH, "utf-8");
  const items = JSON.parse(rawCatalog);

  console.log(`Migrando ${items.length} elementos del catálogo...`);
  if (DRY_RUN) console.log("[dry-run] No se escribirá nada en Firestore.");

  const now = Date.now();
  const destinationCache = new Map();

  for (const item of items) {
    const department = String(item.location ?? "").split(",")[0].trim();
    const destinationId = `dest_${slugify(department)}`;

    if (!destinationCache.has(destinationId)) {
      const coordinates =
        (await geocodeWithGeoapify(department, department)) ?? fallbackCoordinates(department);
      destinationCache.set(destinationId, coordinates);

      const destination = {
        id: destinationId,
        name: department,
        department,
        summary: String(item.description ?? "").slice(0, 1000),
        latitude: coordinates.latitude,
        longitude: coordinates.longitude,
        coverImageUrl: String(item.imageUrl ?? ""),
        categories: [String(item.category ?? "Popular")],
        externalPlaceId: String(coordinates.externalPlaceId ?? ""),
        status: "PUBLICADO",
        createdBy: "migration-script",
        createdAtMillis: now,
        updatedAtMillis: now
      };

      if (!DRY_RUN) {
        await db.collection("destinations").doc(destinationId).set(destination);
      }
      console.log(`  OK destino ${destinationId} (${department})`);
    }

    const packageId = `pkg_${slugify(item.id ?? item.title)}_${destinationId}`;
    const tourPackage = {
      id: packageId,
      destinationId,
      title: String(item.title ?? ""),
      summary: String(item.description ?? ""),
      durationText: String(item.duration ?? ""),
      durationDays: parseDurationDays(item.duration),
      priceAmount: parsePrice(item.price),
      currency: "PEN",
      ratingScore: Number.parseFloat(String(item.rating).replace(",", ".")) || 0,
      category: String(item.category ?? "Popular"),
      coverImageUrl: String(item.imageUrl ?? ""),
      conditions: "Consultar disponibilidad con un agente antes de reservar.",
      includedServiceIds: [],
      status: "PUBLICADO",
      createdBy: "migration-script",
      createdAtMillis: now,
      updatedAtMillis: now
    };

    if (!DRY_RUN) {
      await db.collection("packages").doc(packageId).set(tourPackage);
    }
    console.log(`  OK paquete ${packageId}`);
  }

  console.log("Migración completada.");
}

main().catch((error) => {
  console.error("La migración falló:", error);
  process.exitCode = 1;
});
