// HU05/HU06/HU07: reglas de cotizaciones y reservas.
//
// Estos tests replican los lotes EXACTOS que envía la app:
//   - saveQuotation (agente): batch de 2 escrituras (cotización + solicitud).
//   - observeQuotation (cliente): batch de 3 escrituras (observación +
//     cotización + solicitud).
//   - reviseObservedQuotation (agente): batch de 1 escritura.
//   - acceptQuotation (cliente): DOS tandas — tanda 1 crea la reserva y
//     marca la cotización 'Aceptada'; tanda 2 pone la solicitud 'Aceptada'.
//     La división en dos tandas es intencional: el evaluador de reglas de
//     Firestore tiene un techo de 1000 expresiones COMPARTIDO por solicitud
//     y las tres escrituras en una sola tanda agotaban ese presupuesto.
//
// El cruce reserva ↔ cotización ↔ solicitud exige igualdad exacta de
// fechas, por lo que el seed y los lotes comparten BASE_TIME/DEPARTURE/
// RETURN en lugar de Date.now().
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc, writeBatch } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
let testEnv;

const CLIENT_UID = 'fabian';
const AGENT_UID = 'ismael';
const OUTSIDER_UID = 'otro';
const REQUEST_ID = 'req_nazca';
const QUOTATION_ID = 'cot_1';
// Fechas fijas compartidas entre seed y lotes: el cruce de la reserva
// exige igualdad exacta con la solicitud.
const BASE_TIME = 1_800_000_000_000;
const DEPARTURE = BASE_TIME + 86_400_000;
const RETURN = BASE_TIME + 2 * 86_400_000;

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

const seedQuoted = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', CLIENT_UID), {
      uid: CLIENT_UID, name: 'Fabian', email: 'fabianevs123@gmail.com',
      phone: '987654321', dni: '', city: 'Lima, Perú', role: 'cliente',
      photoUrl: '', preferences: [], updatedAt: Date.now()
    });
    await setDoc(doc(db, 'users', AGENT_UID), {
      uid: AGENT_UID, name: 'Ismael Villegas', email: 'agenteperutours@gmail.com',
      phone: '983201493', dni: '', city: 'Lima, Perú', role: 'agente',
      photoUrl: '', preferences: [], updatedAt: Date.now()
    });
    await setDoc(doc(db, 'users', OUTSIDER_UID), {
      uid: OUTSIDER_UID, name: 'Otro', email: 'otro@x.com', phone: '', dni: '',
      city: 'Lima', role: 'cliente', photoUrl: '', preferences: [],
      updatedAt: Date.now()
    });
    await setDoc(doc(db, 'travel_requests', REQUEST_ID), {
      id: REQUEST_ID,
      userId: CLIENT_UID,
      destination: 'Nazca, Nazca Province, Ica',
      destinationLatitude: -14.8356,
      destinationLongitude: -74.9373,
      originCity: 'Lima, Callao Region, Peru',
      originLatitude: -12.0464,
      originLongitude: -77.0428,
      departureAtMillis: DEPARTURE,
      returnAtMillis: RETURN,
      travelerCount: 4,
      notes: '',
      status: 'Cotizado',
      createdAtMillis: Date.now(),
      quotationId: QUOTATION_ID
    });
    await setDoc(doc(db, 'cotizaciones', QUOTATION_ID), {
      id: QUOTATION_ID,
      requestId: REQUEST_ID,
      clientId: CLIENT_UID,
      clientName: 'Fabian',
      clientEmail: 'fabianevs123@gmail.com',
      destination: 'Nazca, Nazca Province, Ica',
      travelerCount: 4,
      services: [{
        id: 'svc_1', name: 'Tour Nasca Lines', category: 'Tours',
        unitCost: 145, commercialCost: 0, supplierName: '',
        quantity: 4, subtotal: 580
      }],
      baseCost: 580,
      marginPercent: 15,
      totalAmount: 667,
      currency: 'PEN',
      validityDate: '2026-10-28',
      validUntilMillis: Date.now() + 11 * 86_400_000,
      conditions: 'Tarifa garantizada hasta fecha de validez.',
      status: 'Cotizado',
      agentId: AGENT_UID,
      agentName: 'Ismael Villegas',
      version: 1,
      parentQuotationId: '',
      createdAtMillis: Date.now(),
      respondedAtMillis: null,
      latestObservation: '',
      reservationId: ''
    });
  });

// Documento de reserva que replica lo que construye acceptQuotation.
const reservationDoc = (overrides = {}) => ({
  id: QUOTATION_ID,
  quotationId: QUOTATION_ID,
  requestId: REQUEST_ID,
  clientId: CLIENT_UID,
  agentId: AGENT_UID,
  destination: 'Nazca, Nazca Province, Ica',
  originCity: 'Lima, Callao Region, Peru',
  departureAtMillis: DEPARTURE,
  returnAtMillis: RETURN,
  travelerCount: 4,
  services: [{
    id: 'svc_1', name: 'Tour Nasca Lines', category: 'Tours',
    unitCost: 145, commercialCost: 0, supplierName: '',
    quantity: 4, subtotal: 580
  }],
  totalAmount: 667,
  currency: 'PEN',
  validityDate: '2026-10-28',
  conditions: 'Tarifa garantizada hasta fecha de validez.',
  status: 'Confirmada',
  paymentId: '',
  paymentVoucherUrl: '',
  paymentMethod: '',
  paidAtMillis: null,
  createdAtMillis: BASE_TIME,
  ...overrides
});

// Flujo 2: cliente acepta (dos tandas, igual que acceptQuotation).
const clientAcceptTanda1 = (db, respondedAt = Date.now()) => {
  const batch = writeBatch(db);
  batch.set(doc(db, 'reservations', QUOTATION_ID), reservationDoc({
    createdAtMillis: respondedAt
  }));
  batch.update(doc(db, 'cotizaciones', QUOTATION_ID), {
    status: 'Aceptada',
    respondedAtMillis: respondedAt,
    reservationId: QUOTATION_ID
  });
  return batch.commit();
};
const clientAcceptTanda2 = (db) => {
  const batch = writeBatch(db);
  batch.update(doc(db, 'travel_requests', REQUEST_ID), {
    status: 'Aceptada'
  });
  return batch.commit();
};
const clientAcceptTx = async (db) => {
  await clientAcceptTanda1(db);
  return clientAcceptTanda2(db);
};

// Flujo 3: cliente observa (batch exacto de observeQuotation).
const clientObserveTx = (db) => {
  const respondedAt = Date.now();
  const message = '¿El tour incluye almuerzo? Queremos confirmar antes de pagar.';
  const batch = writeBatch(db);
  batch.set(doc(db, 'cotizaciones', QUOTATION_ID, 'observations', 'obs_1'), {
    id: 'obs_1',
    quotationId: QUOTATION_ID,
    authorId: CLIENT_UID,
    authorName: 'Fabian',
    message,
    resultingStatus: 'Observada',
    createdAtMillis: respondedAt
  });
  batch.update(doc(db, 'cotizaciones', QUOTATION_ID), {
    status: 'Observada',
    respondedAtMillis: respondedAt,
    latestObservation: message
  });
  batch.update(doc(db, 'travel_requests', REQUEST_ID), {
    status: 'Observada'
  });
  return batch.commit();
};

// Flujo 4: agente revisa la observacion (reviseObservedQuotation).
const agentReviseBatch = (db) => {
  const batch = writeBatch(db);
  batch.update(doc(db, 'cotizaciones', QUOTATION_ID), {
    services: [{
      id: 'svc_1', name: 'Tour Nasca Lines', category: 'Tours',
      unitCost: 150, commercialCost: 0, supplierName: '',
      quantity: 4, subtotal: 600
    }],
    baseCost: 600,
    marginPercent: 15,
    totalAmount: 690,
    validityDate: '2026-10-30',
    validUntilMillis: Date.now() + 13 * 86_400_000,
    conditions: 'Incluye almuerzo. Tarifa garantizada.',
    status: 'Cotizado',
    version: 2,
    parentQuotationId: QUOTATION_ID
  });
  return batch.commit();
};

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')
    }
  });
});
afterAll(async () => { await testEnv.cleanup(); });

describe('HU05/HU06: cotizaciones y reservas (flujo completo)', () => {
  beforeEach(async () => {
    await testEnv.clearFirestore();
    await seedQuoted();
  });

  it('agente guarda una cotizacion nueva y la solicitud pasa a Cotizado', async () => {
    // Replicar saveQuotation: la cotización se CREA (no se actualiza) y la
    // solicitud, que está 'Pendiente de cotizacion', pasa a 'Cotizado'.
    await testEnv.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await updateDoc(doc(db, 'travel_requests', REQUEST_ID), {
        status: 'Pendiente de cotizacion',
        quotationId: ''
      });
    });
    const db = dbAs(AGENT_UID);
    const batch = writeBatch(db);
    batch.set(doc(db, 'cotizaciones', 'cot_new'), {
      id: 'cot_new',
      requestId: REQUEST_ID,
      clientId: CLIENT_UID,
      clientName: 'Fabian',
      clientEmail: 'fabianevs123@gmail.com',
      destination: 'Nazca, Nazca Province, Ica',
      travelerCount: 4,
      services: [{
        id: 'svc_1', name: 'Tour Nasca Lines', category: 'Tours',
        unitCost: 145, commercialCost: 0, supplierName: '',
        quantity: 4, subtotal: 580
      }],
      baseCost: 580,
      marginPercent: 15,
      totalAmount: 667,
      currency: 'PEN',
      validityDate: '2026-10-28',
      validUntilMillis: Date.now() + 11 * 86_400_000,
      conditions: 'Tarifa garantizada hasta fecha de validez.',
      status: 'Cotizado',
      agentId: AGENT_UID,
      agentName: 'Ismael Villegas',
      version: 1,
      parentQuotationId: '',
      createdAtMillis: Date.now(),
      respondedAtMillis: null,
      latestObservation: '',
      reservationId: ''
    });
    batch.update(doc(db, 'travel_requests', REQUEST_ID), {
      status: 'Cotizado',
      quotationId: 'cot_new'
    });
    await assertSucceeds(batch.commit());
  });

  it('agente revisa la cotizacion observada (nueva version)', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), 'cotizaciones', QUOTATION_ID), {
        status: 'Observada',
        respondedAtMillis: Date.now(),
        latestObservation: '¿El tour incluye almuerzo? Queremos confirmar antes de pagar.'
      });
    });
    await assertSucceeds(agentReviseBatch(dbAs(AGENT_UID)));
  });

  it('cliente observa la cotizacion', async () => {
    await assertSucceeds(clientObserveTx(dbAs(CLIENT_UID)));
  });

  it('cliente acepta la cotizacion y crea la reserva', async () => {
    await assertSucceeds(clientAcceptTx(dbAs(CLIENT_UID)));
  });

  it('el agente NO puede aceptar la cotizacion en nombre del cliente', async () => {
    await assertFails(clientAcceptTx(dbAs(AGENT_UID)));
  });

  it('un usuario ajeno NO puede responder la cotizacion', async () => {
    await assertFails(clientAcceptTx(dbAs(OUTSIDER_UID)));
  });

  it('el cliente NO puede crear la reserva con un importe distinto a la cotizacion', async () => {
    const db = dbAs(CLIENT_UID);
    const respondedAt = Date.now();
    const batch = writeBatch(db);
    batch.set(doc(db, 'reservations', QUOTATION_ID),
      reservationDoc({ totalAmount: 999_999, createdAtMillis: respondedAt }));
    batch.update(doc(db, 'cotizaciones', QUOTATION_ID), {
      status: 'Aceptada',
      respondedAtMillis: respondedAt,
      reservationId: QUOTATION_ID
    });
    await assertFails(batch.commit());
  });

  it('el cliente NO puede saltarse la cotizacion: la solicitud no pasa a Aceptada sin cotizacion aceptada', async () => {
    await assertFails(clientAcceptTanda2(dbAs(CLIENT_UID)));
  });

  it('el cliente NO puede marcar su solicitud como Cotizado (solo el agente cotiza)', async () => {
    const db = dbAs(CLIENT_UID);
    const batch = writeBatch(db);
    batch.update(doc(db, 'travel_requests', REQUEST_ID), {
      status: 'Cotizado'
    });
    await assertFails(batch.commit());
  });
});
