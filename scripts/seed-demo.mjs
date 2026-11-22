import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { randomBytes, randomUUID } from "node:crypto";
import { resolve } from "node:path";
import { openDatabase, root, planPatch, emptyPackageSnapshot } from "./firebase-maintenance.mjs";

// Datos simulados: IDs estables, usuarios propios y sin token FCM.
// La segunda ejecución conserva los documentos que el equipo ya haya editado.
const db = await openDatabase();
const collections = [
  "users", "destinations", "packages", "catalog_services", "travel_requests",
  "cotizaciones", "reservations", "payments", "trip_itinerary_items", "trip_checklist_items"
];
const records = Object.fromEntries(await Promise.all(
  collections.map(async (name) => [name, await db.list(name)])
));
const backup = structuredClone(records);
const changes = [];
const now = Date.now();
const day = 86_400_000;
const agent = records.users.find((r) => r.data.email === "agenteperutours@gmail.com")
  ?? records.users.find((r) => r.data.role === "agente");
if (!agent) throw new Error("Se necesita un agente existente para los ejemplos.");

const profiles = [
  { uid: "demo_perutours_client_01", name: "Lucía Demo", role: "cliente", email: "demo.cliente1@perutours.example" },
  { uid: "demo_perutours_client_02", name: "Mateo Demo", role: "cliente", email: "demo.cliente2@perutours.example" },
  { uid: "demo_perutours_manager", name: "Gerencia Demo", role: "gerente", email: "demo.gerencia@perutours.example" },
  { uid: "demo_perutours_agent", name: "Agente Demo", role: "agente", email: "demo.agente@perutours.example" }
];
const credentialsPath = resolve(root, ".artifacts/demo-accounts.json");
const credentials = existsSync(credentialsPath) ? JSON.parse(readFileSync(credentialsPath)) : [];
let voucherUrl = "";
if (process.argv.includes("--apply")) {
  const { auth } = await db.adminServices();
  for (const profile of profiles) {
    try {
      const user = await auth.getUser(profile.uid);
      if (user.email !== profile.email) throw new Error("El UID de demostración ya pertenece a otra cuenta.");
    } catch (error) {
      if (error.code !== "auth/user-not-found") throw error;
      const password = "PeruTours!" + randomBytes(12).toString("base64url");
      await auth.createUser({
        uid: profile.uid, email: profile.email, displayName: profile.name,
        password, emailVerified: true
      });
      credentials.push({ email: profile.email, password, role: profile.role });
      mkdirSync(resolve(root, ".artifacts"), { recursive: true });
      // Se escribe después de cada alta para no perder la contraseña si se interrumpe.
      writeFileSync(credentialsPath, JSON.stringify(credentials, null, 2));
    }
  }
  const objectPath = "demo/vouchers/comprobante-simulado.png";
  const token = await db.ensureDemoImage(objectPath,
    readFileSync(resolve(root, "scripts/seed/demo-voucher.png")), randomUUID());
  voucherUrl = "https://firebasestorage.googleapis.com/v0/b/" + db.bucketName +
    "/o/" + encodeURIComponent(objectPath) + "?alt=media&token=" + token;
}

const insert = (collection, id, data) => {
  if (records[collection].some((r) => r.id === id)) return;
  const record = { id, data: {} };
  planPatch(changes, collection, record, data);
  records[collection].push(record);
};
for (const profile of profiles) {
  insert("users", profile.uid, {
    ...profile, phone: "", dni: "", city: "Lima, Perú", photoUrl: "",
    preferences: [], tripType: "Cultura", budget: "Medio", duration: "3-5 días", updatedAt: now
  });
}

const scenarios = [
  { key: "01", city: "Cusco", state: "pending", package: true },
  { key: "02", city: "Arequipa", state: "pending", activity: "arequipa_colca" },
  { key: "03", city: "Ica", state: "quoted", package: true },
  { key: "04", city: "Puno", state: "observed", package: true },
  { key: "05", city: "Cusco", state: "accepted", package: true },
  { key: "06", city: "Arequipa", state: "paid", package: true },
  { key: "07", city: "Ica", state: "review", package: true },
  { key: "08", city: "Puno", state: "paid", package: true }
];
for (let index = 0; index < scenarios.length; index++) {
  const scenario = scenarios[index];
  const prefix = "demo_journey_" + scenario.key;
  const destination = records.destinations.find((r) => r.data.name === scenario.city);
  const pkg = scenario.package && records.packages.find((r) => r.data.destinationId === destination?.id);
  if (!destination || (scenario.package && !pkg)) throw new Error("Falta catálogo para " + scenario.city);
  const client = profiles[index % 2];
  const createdAt = now - (index + 1) * day;
  const departure = now + (index + 7) * day;
  const snapshot = pkg ? Object.fromEntries(Object.keys(emptyPackageSnapshot())
    .map((key) => [key, pkg.data[key]])) : emptyPackageSnapshot();
  const requestId = prefix + "_request";
  const quotationId = prefix + "_quotation";
  const hasQuote = scenario.state !== "pending";
  const accepted = ["accepted", "paid", "review"].includes(scenario.state);
  const status = !hasQuote ? "Pendiente de cotizacion"
    : accepted ? "Aceptada" : scenario.state === "observed" ? "Observada" : "Cotizado";
  const request = {
    id: requestId, userId: client.uid, requestType: pkg ? "PACKAGE" : "CUSTOM",
    packageId: pkg?.id ?? "", destinationId: destination.id, packageSnapshot: snapshot,
    requestedServiceIds: scenario.activity ? [scenario.activity] : [],
    destination: scenario.city + ", Perú",
    destinationLatitude: destination.data.latitude, destinationLongitude: destination.data.longitude,
    originCity: "Lima, Perú", originLatitude: -12.0464, originLongitude: -77.0428,
    departureAtMillis: departure, returnAtMillis: departure + 3 * day, travelerCount: 2,
    notes: "Ejemplo simulado " + scenario.key + " para la exposición. No es una compra real.",
    status, createdAtMillis: createdAt, quotationId: hasQuote ? quotationId : "",
    assignedAgentId: hasQuote ? agent.id : "", claimedAtMillis: hasQuote ? createdAt : null
  };
  insert("travel_requests", requestId, request);
  if (!hasQuote) continue;
  const services = snapshot.includedServiceIds.map((id) => records.catalog_services.find((r) => r.id === id))
    .filter(Boolean).map(({ id, data: service }) => ({
      id, catalogServiceId: id, destinationId: destination.id, providerId: service.providerId,
      name: service.name, category: service.category, unitCost: service.unitCost,
      commercialCost: service.commercialCost ?? 0, supplierName: service.supplierName,
      description: service.description, requiresScheduling: service.requiresScheduling,
      quantity: 2, subtotal: service.unitCost * 2
    }));
  if (!services.length || services.length > 6) throw new Error("Servicios incompletos en el ejemplo " + prefix);
  const baseCost = services.reduce((total, service) => total + service.subtotal, 0);
  const totalAmount = Math.round(baseCost * 1.15 * 100) / 100;
  const observation = "¿Podemos empezar la primera actividad a las nueve de la mañana?";
  const quotation = {
    id: quotationId, requestId, clientId: client.uid, clientName: client.name, clientEmail: client.email,
    destination: request.destination, travelerCount: 2, services, baseCost, marginPercent: 15,
    totalAmount, currency: "PEN", validityDate: new Date(now + 14 * day).toISOString().slice(0, 10),
    validUntilMillis: now + 14 * day, conditions: "Cotización simulada para demostración académica.",
    status: accepted ? "Aceptada" : scenario.state === "observed" ? "Observada" : "Cotizado",
    agentId: agent.id, agentName: agent.data.name, createdAtMillis: createdAt,
    respondedAtMillis: accepted || scenario.state === "observed" ? createdAt + 60_000 : null,
    latestObservation: scenario.state === "observed" ? observation : "",
    reservationId: accepted ? quotationId : "", version: 1, parentQuotationId: "",
    packageId: pkg?.id ?? "", destinationId: destination.id, packageSnapshot: snapshot
  };
  insert("cotizaciones", quotationId, quotation);
  if (scenario.state === "observed") {
    const path = "cotizaciones/" + quotationId + "/observations";
    records[path] = await db.list(path);
    backup[path] = structuredClone(records[path]);
    insert(path, prefix + "_observation", {
      id: prefix + "_observation", quotationId, authorId: client.uid,
      authorName: client.name, message: observation, resultingStatus: "Observada",
      createdAtMillis: createdAt + 60_000
    });
  }
  if (!accepted) continue;
  const paymentId = prefix + "_payment";
  const paid = scenario.state === "paid";
  const review = scenario.state === "review";
  insert("reservations", quotationId, {
    id: quotationId, quotationId, requestId, clientId: client.uid, agentId: agent.id,
    packageId: quotation.packageId, destinationId: destination.id, packageSnapshot: snapshot,
    destination: request.destination, originCity: request.originCity,
    departureAtMillis: request.departureAtMillis, returnAtMillis: request.returnAtMillis,
    travelerCount: 2, services, totalAmount, currency: "PEN", validityDate: quotation.validityDate,
    conditions: quotation.conditions, status: paid ? "Pagado" : review ? "Pago enviado" : "Confirmada",
    createdAtMillis: createdAt + 60_000, paymentId: paid || review ? paymentId : "",
    paymentVoucherUrl: "", paymentMethod: "", paidAtMillis: null
  });
  if (paid || review) {
    insert("payments", paymentId, {
      id: paymentId, reservationId: quotationId, quotationId, clientId: client.uid, agentId: agent.id,
      attemptNumber: 1, previousPaymentId: "", method: "Yape", amount: totalAmount, currency: "PEN",
      voucherUrl, status: paid ? "Aprobado" : "Pendiente", rejectionReason: "",
      submittedAtMillis: createdAt + 120_000, reviewedAtMillis: paid ? createdAt + 180_000 : null,
      reviewedBy: paid ? agent.id : ""
    });
  }
  if (paid) {
    services.filter((service) => service.requiresScheduling).forEach((service, order) => {
      const id = prefix + "_activity_" + order;
      insert("trip_itinerary_items", id, {
        id, reservationId: quotationId, reservationServiceId: service.id,
        clientId: client.uid, agentId: agent.id, title: service.name,
        description: service.description, category: service.category === "Transporte" ? "Transporte" : "Actividad",
        locationName: request.destination, latitude: request.destinationLatitude, longitude: request.destinationLongitude,
        startAtMillis: departure + order * 3_600_000, endAtMillis: departure + (order + 1) * 3_600_000,
        dayIndex: 1, sortOrder: order, providerName: service.supplierName, status: "Programado", notes: "",
        source: "reservation", createdAtMillis: createdAt, updatedAtMillis: createdAt
      });
    });
    insert("trip_checklist_items", prefix + "_checklist", {
      id: prefix + "_checklist", reservationId: quotationId, clientId: client.uid,
      title: "Llevar documento de identidad", category: "Documentos", isCompleted: false,
      createdAtMillis: createdAt, updatedAtMillis: createdAt
    });
  }
}
console.log("Ejemplos nuevos por colección:", changes.reduce((counts, change) => ({
  ...counts, [change.collection]: (counts[change.collection] ?? 0) + 1
}), {}));
await db.apply(changes, backup, "seed-demo");
if (credentials.length) console.log("Credenciales de las cuentas nuevas: .artifacts/demo-accounts.json");
