import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let testEnv;

const CLIENT_UID = 'cliente_viaje_1';
const OTHER_CLIENT_UID = 'cliente_viaje_2';
const AGENT_UID = 'agente_viaje_1';
const REQUEST_ID = 'req_viaje_1';

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

const seedUsers = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [uid, role] of [
      [CLIENT_UID, 'cliente'],
      [OTHER_CLIENT_UID, 'cliente'],
      [AGENT_UID, 'agente']
    ]) {
      await setDoc(doc(db, 'users', uid), {
        uid,
        name: 'Usuario ' + uid,
        email: uid + '@perutours.pe',
        phone: '900000000',
        dni: '',
        city: 'Lima, Perú',
        role,
        photoUrl: '',
        preferences: [],
        updatedAt: Date.now()
      });
    }
  });

// Documento EXACTO que la app serializa al enviar la solicitud
// (TravelRequest incluye siempre quotationId, vacío por defecto).
const validRequest = (overrides = {}) => ({
  id: REQUEST_ID,
  userId: CLIENT_UID,
  destination: 'Arequipa, Perú',
  destinationLatitude: -16.409047,
  destinationLongitude: -71.537451,
  originCity: 'Lima, Perú',
  originLatitude: -12.0464,
  originLongitude: -77.0428,
  departureAtMillis: Date.now() + 86_400_000,
  returnAtMillis: Date.now() + 2 * 86_400_000,
  travelerCount: 2,
  notes: 'Preferimos hotel céntrico.',
  status: 'Pendiente de cotizacion',
  createdAtMillis: Date.now(),
  quotationId: '',
  ...overrides
});

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')
    }
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await seedUsers();
});

describe('Envío de la solicitud de viaje por el cliente', () => {
  it('permite crear la solicitud con quotationId vacío (como la app)', async () => {
    await assertSucceeds(setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID), validRequest()));
  });

  it('permite crear la solicitud sin el campo quotationId', async () => {
    const { quotationId, ...withoutQuotationId } = validRequest();
    await assertSucceeds(
      setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID), withoutQuotationId)
    );
  });

  it('rechaza una solicitud con un quotationId asignado por el cliente', async () => {
    await assertFails(
      setDoc(
        doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
        validRequest({ quotationId: 'cotizacion_hack_1' })
      )
    );
  });

  it('rechaza una solicitud enviada por otro cliente (userId ajeno)', async () => {
    await assertFails(
      setDoc(doc(dbAs(OTHER_CLIENT_UID), 'travel_requests', REQUEST_ID), validRequest())
    );
  });

  it('rechaza una solicitud enviada por un agente', async () => {
    await assertFails(
      setDoc(doc(dbAs(AGENT_UID), 'travel_requests', REQUEST_ID), validRequest())
    );
  });

  it('rechaza una solicitud con fechas invertidas', async () => {
    const now = Date.now();
    await assertFails(
      setDoc(
        doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
        validRequest({
          departureAtMillis: now + 2 * 86_400_000,
          returnAtMillis: now + 86_400_000
        })
      )
    );
  });

  it('rechaza una solicitud con estado inventado', async () => {
    await assertFails(
      setDoc(
        doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
        validRequest({ status: 'Pagada' })
      )
    );
  });
});
