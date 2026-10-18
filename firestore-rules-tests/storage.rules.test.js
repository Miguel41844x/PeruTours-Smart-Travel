import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { doc, setDoc } from 'firebase/firestore';
import { getBytes, ref, uploadBytes } from 'firebase/storage';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let testEnv;

const CLIENT_UID = 'cliente_storage_1';
const OTHER_CLIENT_UID = 'cliente_storage_2';
const AGENT_UID = 'agente_storage_1';
const RESERVATION_ID = 'res_storage_1';

// Ruta exacta que usa UploadPaymentWorker al subir el comprobante.
const voucherPath = (uid = CLIENT_UID) =>
  `payment_vouchers/${uid}/${RESERVATION_ID}/voucher.jpg`;

const uploadVoucher = (
  uid,
  {
    path = voucherPath(uid),
    contentType = 'image/jpeg',
    body = Buffer.from('comprobante')
  } = {}
) =>
  uploadBytes(
    ref(testEnv.authenticatedContext(uid).storage(), path),
    body,
    { contentType }
  );

const downloadVoucher = (uid, path = voucherPath()) =>
  getBytes(ref(testEnv.authenticatedContext(uid).storage(), path));

const seedReservation = (overrides = {}) =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'reservations', RESERVATION_ID), {
      id: RESERVATION_ID,
      clientId: CLIENT_UID,
      agentId: AGENT_UID,
      status: 'Confirmada',
      ...overrides
    });
  });

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')
    },
    storage: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'storage.rules'), 'utf8')
    }
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.clearStorage();
  await seedReservation();
});

describe('Subida del comprobante a Storage', () => {
  it('permite al cliente subir el voucher de su reserva Confirmada', async () => {
    await assertSucceeds(uploadVoucher(CLIENT_UID));
  });

  it('permite reenviar el voucher cuando el pago fue rechazado', async () => {
    await seedReservation({ status: 'Pago rechazado' });
    await assertSucceeds(uploadVoucher(CLIENT_UID));
  });

  it('rechaza el voucher si la reserva no existe', async () => {
    await testEnv.clearFirestore();
    await assertFails(uploadVoucher(CLIENT_UID));
  });

  it('rechaza el voucher si la reserva pertenece a otro cliente', async () => {
    await seedReservation({ clientId: OTHER_CLIENT_UID });
    await assertFails(uploadVoucher(CLIENT_UID));
  });

  it('rechaza el voucher si la reserva ya no admite pago', async () => {
    await seedReservation({ status: 'Pagado' });
    await assertFails(uploadVoucher(CLIENT_UID));
  });

  it('rechaza subir en la carpeta de otro usuario', async () => {
    await assertFails(uploadVoucher(OTHER_CLIENT_UID));
  });

  it('rechaza tipos de archivo que no sean imágenes', async () => {
    await assertFails(uploadVoucher(CLIENT_UID, { contentType: 'application/pdf' }));
  });
});

describe('Lectura del comprobante en Storage', () => {
  beforeEach(async () => {
    await assertSucceeds(uploadVoucher(CLIENT_UID));
  });

  it('permite al cliente leer su propio comprobante', async () => {
    await assertSucceeds(downloadVoucher(CLIENT_UID));
  });

  it('permite al agente asignado leer el comprobante de la reserva', async () => {
    await assertSucceeds(downloadVoucher(AGENT_UID));
  });

  it('rechaza que otro cliente lea el comprobante', async () => {
    await assertFails(downloadVoucher(OTHER_CLIENT_UID));
  });
});
