import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { collection, doc, getDocs, setDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const MANAGER_UID = 'gerente_1';
const CLIENT_UID = 'cliente_dashboard_1';

let testEnv;

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

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
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', MANAGER_UID), { role: 'gerente' });
    await setDoc(doc(db, 'users', CLIENT_UID), { role: 'cliente' });
    await setDoc(doc(db, 'cotizaciones', 'quote_1'), {
      clientId: 'otro_cliente', agentId: 'agente_1', status: 'Cotizado'
    });
    await setDoc(doc(db, 'payments', 'payment_1'), {
      clientId: 'otro_cliente', agentId: 'agente_1', status: 'Aprobado'
    });
    await setDoc(doc(db, 'travel_requests', 'request_1'), {
      userId: 'otro_cliente', destination: 'Cusco, Perú'
    });
  });
});

describe('Lectura gerencial para HU09', () => {
  it('permite al gerente consultar las colecciones del dashboard', async () => {
    const db = dbAs(MANAGER_UID);

    await assertSucceeds(getDocs(collection(db, 'cotizaciones')));
    await assertSucceeds(getDocs(collection(db, 'payments')));
    await assertSucceeds(getDocs(collection(db, 'travel_requests')));
  });

  it('rechaza que un cliente consulte información comercial global', async () => {
    const db = dbAs(CLIENT_UID);

    await assertFails(getDocs(collection(db, 'cotizaciones')));
    await assertFails(getDocs(collection(db, 'payments')));
    await assertFails(getDocs(collection(db, 'travel_requests')));
  });
});
