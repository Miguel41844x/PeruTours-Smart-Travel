import fs from 'node:fs';
import { initializeTestEnvironment } from '@firebase/rules-unit-testing';
import { createMockUserToken } from '@firebase/util';
import { doc, setDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, expect, it } from 'vitest';

let env;
const uid = 'client_numeric_snapshot';
const emptySnapshot = () => ({
  title: '', summary: '', durationText: '', durationDays: 0, priceAmount: 0,
  currency: 'PEN', coverImageUrl: '', conditions: '', includedServiceIds: []
});
const fields = (data) => Object.fromEntries(Object.entries(data).map(([key, value]) => [
  key, typeof value === 'number' && key === 'priceAmount' ? { doubleValue: value } : encode(value)
]));
const encode = (value) => {
  if (value === null) return { nullValue: null };
  if (typeof value === 'string') return { stringValue: value };
  if (typeof value === 'number') return Number.isInteger(value)
    ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  return { mapValue: { fields: fields(value) } };
};

beforeAll(async () => {
  env = await initializeTestEnvironment({
    firestore: { rules: fs.readFileSync('../firestore.rules', 'utf8') }
  });
});
afterAll(async () => { await env.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

it.each(['CUSTOM', 'PACKAGE'])('acepta priceAmount Double serializado por Android en %s', async (type) => {
  const snapshot = type === 'CUSTOM' ? emptySnapshot() : {
    ...emptySnapshot(), title: 'Amazonía de demostración', durationText: '3 días', durationDays: 3,
    priceAmount: 1050, includedServiceIds: Array.from({ length: 6 }, (_, i) => 'service_' + i)
  };
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', uid), { uid, role: 'cliente' });
    await setDoc(doc(db, 'destinations', 'dest_demo'), { status: 'PUBLICADO' });
    // El SDK de JavaScript guarda 1050 como integerValue. Android envía Double.
    await setDoc(doc(db, 'packages', 'pkg_demo'), {
      ...snapshot, destinationId: 'dest_demo', status: 'PUBLICADO'
    });
  });
  const now = Date.now();
  const request = {
    id: 'numeric_request', userId: uid, requestType: type,
    packageId: type === 'PACKAGE' ? 'pkg_demo' : '', destinationId: 'dest_demo',
    packageSnapshot: snapshot, requestedServiceIds: [], destination: 'Iquitos, Perú',
    destinationLatitude: -3.749, destinationLongitude: -73.244,
    originCity: 'Lima, Perú', originLatitude: -12.0464, originLongitude: -77.0428,
    departureAtMillis: now + 86_400_000, returnAtMillis: now + 172_800_000,
    travelerCount: 2, notes: '', status: 'Pendiente de cotizacion', createdAtMillis: now,
    quotationId: '', assignedAgentId: '', claimedAtMillis: null
  };
  const token = createMockUserToken({ sub: uid }, env.projectId);
  const database = 'projects/' + env.projectId + '/databases/(default)/documents';
  const response = await fetch('http://' + process.env.FIRESTORE_EMULATOR_HOST + '/v1/' + database + ':commit', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
    body: JSON.stringify({ writes: [{
      update: { name: database + '/travel_requests/numeric_request', fields: fields(request) }
    }] })
  });
  const body = await response.json();
  expect(response.status, body.error?.message).toBe(200);
});
