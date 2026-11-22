import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import {
  openDatabase, root, normalize, slug, planPatch, emptyPackageSnapshot
} from "./firebase-maintenance.mjs";

const db = await openDatabase();
const names = [
  "users", "destinations", "providers", "catalog_services", "packages",
  "travel_requests", "cotizaciones", "reservations", "payments"
];
const records = Object.fromEntries(await Promise.all(
  names.map(async (name) => [name, await db.list(name)])
));
const backup = structuredClone(records);
const changes = [];
const now = Date.now();
const links = JSON.parse(readFileSync(resolve(root, "scripts/seed/catalog-links.json")));
const owner = records.users.find((r) => r.data.role === "administrador")?.id;
if (!owner) throw new Error("Se necesita un administrador existente para mantener el catálogo.");

const key = readFileSync(resolve(root, "local.properties"), "utf8")
  .match(/^GEOAPIFY_API_KEY\s*=\s*(.+)$/m)?.[1].trim();
const destinationFor = (city) => {
  const first = normalize(city).split(",")[0].trim();
  return records.destinations.find((r) => normalize(r.data.name) === first);
};
const isSameArea = (request, destination) => {
  const values = [request.destinationLatitude, request.destinationLongitude,
    destination?.data.latitude, destination?.data.longitude];
  if (!values.every(Number.isFinite)) return false;
  const [latitude, longitude, targetLatitude, targetLongitude] = values.map((value) => value * Math.PI / 180);
  const arc = Math.sin((targetLatitude - latitude) / 2) ** 2 +
    Math.cos(latitude) * Math.cos(targetLatitude) * Math.sin((targetLongitude - longitude) / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(arc), Math.sqrt(1 - arc)) <= 300;
};
const incompatibleHistoricalRequests = new Set();

for (const city of [...new Set(records.catalog_services.map((r) => r.data.city))]) {
  if (destinationFor(city)) continue;
  if (!key) throw new Error("Configura Geoapify para validar los destinos faltantes.");
  const query = new URLSearchParams({
    text: city + ", Perú", filter: "countrycode:pe", limit: "1", lang: "es", apiKey: key
  });
  const response = await fetch("https://api.geoapify.com/v1/geocode/search?" + query, {
    signal: AbortSignal.timeout(15000)
  });
  if (!response.ok) throw new Error("Geoapify rechazó la validación de " + city);
  const place = (await response.json()).features?.[0];
  if (!place || place.properties.country_code !== "pe") {
    throw new Error("No pudimos validar " + city + " dentro del Perú.");
  }
  const id = "dest_" + slug(city);
  const record = { id, data: {} };
  planPatch(changes, "destinations", record, {
    id, name: city, department: place.properties.state ?? city,
    summary: "Destino peruano con servicios turísticos disponibles en el catálogo.",
    latitude: place.geometry.coordinates[1], longitude: place.geometry.coordinates[0],
    coverImageUrl: "", categories: [], externalPlaceId: place.properties.place_id,
    status: "BORRADOR", createdBy: owner, createdAtMillis: now, updatedAtMillis: now
  });
  records.destinations.push(record);
}

for (const record of records.catalog_services) {
  const service = record.data;
  const destination = destinationFor(service.city);
  if (!destination) throw new Error("Falta el destino del servicio " + record.id);
  let provider = records.providers.find((r) => r.id === service.providerId);
  if (!provider) {
    const id = "demo_provider_" + slug(service.city);
    provider = records.providers.find((r) => r.id === id);
    if (!provider) {
      provider = { id, data: {} };
      planPatch(changes, "providers", provider, {
        id, name: "Operador de demostración de " + service.city, contactName: "Equipo de demostración",
        phone: "", email: "catalogo@perutours.example", city: service.city, active: true,
        createdBy: owner, createdAtMillis: now, updatedAtMillis: now
      });
      records.providers.push(provider);
    }
  }
  planPatch(changes, "catalog_services", record, {
    id: record.id, destinationId: destination.id, providerId: provider.id,
    supplierName: service.supplierName || provider.data.name,
    defaultQuantity: service.defaultQuantity ?? 1,
    durationMinutes: service.durationMinutes ?? 60,
    requiresScheduling: service.requiresScheduling ?? !/alojamiento|seguro/i.test(service.category),
    requestable: service.requestable ?? /tours|aventura|experiencias/i.test(service.category),
    imageUrl: service.imageUrl ?? ""
  });
}

// El paquete de Máncora usaba servicios de Piura ciudad. Los ejemplos nuevos
// tienen identificadores propios y se anuncian explícitamente como simulados.
const mancora = destinationFor("Máncora");
if (mancora) {
  const providerId = "demo_provider_mancora";
  if (!records.providers.some((r) => r.id === providerId)) {
    const provider = { id: providerId, data: {} };
    planPatch(changes, "providers", provider, {
      id: providerId, name: "Operador de demostración de Máncora",
      contactName: "Equipo de demostración", phone: "", email: "catalogo@perutours.example",
      city: "Máncora", active: true, createdBy: owner,
      createdAtMillis: now, updatedAtMillis: now
    });
    records.providers.push(provider);
  }
  for (const example of links.mancoraServices) {
    if (records.catalog_services.some((r) => r.id === example.id)) continue;
    const service = { id: example.id, data: {} };
    planPatch(changes, "catalog_services", service, {
      ...example, defaultQuantity: 1, city: "Máncora", destinationId: mancora.id, providerId,
      description: "Servicio de demostración para probar la aplicación; sujeto a cotización.",
      supplierName: "Operador de demostración de Máncora", commercialCost: 0,
      imageUrl: "", active: true
    });
    records.catalog_services.push(service);
  }
}

for (const record of records.packages) {
  const pkg = record.data;
  let includedServiceIds = pkg.includedServiceIds ?? [];
  if (!includedServiceIds.length) {
    includedServiceIds = links.legacyPackageServices[record.id] ?? [];
  }
  if (record.id === links.mancoraPackageId && includedServiceIds.some((id) => id.startsWith("piura_"))) {
    includedServiceIds = links.mancoraServices.map((service) => service.id);
  }
  const unresolved = includedServiceIds.filter((id) =>
    !records.catalog_services.some((r) => r.id === id && r.data.destinationId === pkg.destinationId)
  );
  if (unresolved.length) {
    throw new Error("Paquete " + record.id + ": referencias ajenas al destino; corrige antes de aplicar.");
  }
  planPatch(changes, "packages", record, {
    id: record.id, includedServiceIds,
    conditions: pkg.conditions ?? "Disponibilidad y condiciones sujetas a la propuesta del agente."
  });
}

for (const record of records.users) {
  planPatch(changes, "users", record, {
    uid: record.id, role: record.data.role || "cliente",
    preferences: Array.isArray(record.data.preferences) ? record.data.preferences : [],
    tripType: record.data.tripType ?? "", budget: record.data.budget ?? "", duration: record.data.duration ?? ""
  });
}

for (const record of records.travel_requests) {
  const request = record.data;
  const candidate = records.destinations.find((r) => r.id === request.destinationId)
    ?? destinationFor(request.destination);
  const incompatible = !request.packageId && candidate && !isSameArea(request, candidate);
  if (incompatible) {
    incompatibleHistoricalRequests.add(record.id);
    console.log("Revisar ubicación histórica: " + record.id + ". Falta confirmar la ubicación; se conserva el lugar original sin asociarlo a otro destino.");
  }
  const quotations = records.cotizaciones.filter((r) => r.data.requestId === record.id)
    .sort((a, b) => b.data.createdAtMillis - a.data.createdAtMillis);
  const latest = quotations[0];
  planPatch(changes, "travel_requests", record, {
    id: record.id, requestType: request.requestType ?? "CUSTOM",
    packageId: request.packageId ?? "",
    destinationId: incompatible ? "" : request.destinationId ?? candidate?.id ?? "",
    packageSnapshot: request.packageSnapshot ?? emptyPackageSnapshot(),
    requestedServiceIds: request.requestedServiceIds ?? [],
    assignedAgentId: request.assignedAgentId || latest?.data.agentId || "",
    claimedAtMillis: request.claimedAtMillis ?? (latest ? latest.data.createdAtMillis : null)
  });
}

for (const name of ["cotizaciones", "reservations"]) {
  for (const record of records[name]) {
    const request = records.travel_requests.find((r) => r.id === record.data.requestId)?.data;
    planPatch(changes, name, record, {
      id: record.id, packageId: record.data.packageId ?? request?.packageId ?? "",
      destinationId: incompatibleHistoricalRequests.has(record.data.requestId)
        ? "" : record.data.destinationId ?? request?.destinationId ?? "",
      packageSnapshot: record.data.packageSnapshot ?? request?.packageSnapshot ?? emptyPackageSnapshot()
    });
  }
}
for (const record of records.payments) {
  planPatch(changes, "payments", record, {
    id: record.id, attemptNumber: record.data.attemptNumber ?? 1,
    previousPaymentId: record.data.previousPaymentId ?? ""
  });
}
console.log("Cambios por colección:", changes.reduce((counts, r) => ({
  ...counts, [r.collection]: (counts[r.collection] ?? 0) + 1
}), {}));
await db.apply(changes, backup, "repair-data");
