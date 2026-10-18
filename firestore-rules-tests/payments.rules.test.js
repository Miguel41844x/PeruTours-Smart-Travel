import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import {
  doc,
  getDoc,
  deleteDoc,
  setDoc,
  updateDoc,
  writeBatch
} from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let testEnv;

const CLIENT_UID = 'cliente_1';
const AGENT_UID = 'agente_1';
const OTHER_AGENT_UID = 'agente_2';
const OTHER_CLIENT_UID = 'cliente_2';
const RESERVATION_ID = 'res_1';

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

const seedData = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', CLIENT_UID), {
      uid: CLIENT_UID, name: 'Juan Pérez', email: 'cliente@perutours.pe',
      phone: '987654321', dni: '', city: 'Lima, Perú', role: 'cliente',
      photoUrl: '', preferences: [], updatedAt: Date.now()
    });
    await setDoc(doc(db, 'users', OTHER_CLIENT_UID), {
      uid: OTHER_CLIENT_UID, name: 'Ana Torres', email: 'otro@perutours.pe',
      phone: '912345678', dni: '', city: 'Arequipa', role: 'cliente',
      photoUrl: '', preferences: [], updatedAt: Date.now()
    });
    for (const agentUid of [AGENT_UID, OTHER_AGENT_UID]) {
      await setDoc(doc(db, 'users', agentUid), {
        uid: agentUid, name: 'Agente ' + agentUid, email: agentUid + '@perutours.pe',
        phone: '900000000', dni: '', city: 'Lima, Perú', role: 'agente',
        photoUrl: '', preferences: [], updatedAt: Date.now()
      });
    }
    await setDoc(doc(db, 'reservations', RESERVATION_ID), validReservation());
  });

const validReservation = (overrides = {}) => ({
  id: RESERVATION_ID,
  quotationId: RESERVATION_ID,
  requestId: 'req_1',
  clientId: CLIENT_UID,
  agentId: AGENT_UID,
  destination: 'Cusco',
  originCity: 'Lima',
  departureAtMillis: Date.now() + 86_400_000,
  returnAtMillis: Date.now() + 172_800_000,
  travelerCount: 2,
  services: [
    { id: 'svc_1', name: 'Tour Machu Picchu', category: 'Excursión', unitCost: 750, quantity: 2, subtotal: 1500 }
  ],
  totalAmount: 1500,
  currency: 'PEN',
  validityDate: '2026-12-31',
  conditions: '',
  status: 'Confirmada',
  createdAtMillis: Date.now() - 60_000,
  paymentId: '',
  paymentVoucherUrl: '',
  paymentMethod: '',
  paidAtMillis: null,
  ...overrides
});

const validPayment = (overrides = {}) => ({
  id: RESERVATION_ID,
  reservationId: RESERVATION_ID,
  quotationId: RESERVATION_ID,
  clientId: CLIENT_UID,
  agentId: AGENT_UID,
  method: 'Yape',
  amount: 1500,
  currency: 'PEN',
  voucherUrl: 'https://firebasestorage.googleapis.com/v0/b/x/voucher.jpg',
  status: 'Pendiente',
  rejectionReason: '',
  submittedAtMillis: Date.now(),
  reviewedAtMillis: null,
  reviewedBy: '',
  ...overrides
});

// Cliente envía comprobante: crea el pago y mueve la reserva a 'Pago enviado'.
const submitPaymentBatch = (db, { payment = {}, reservation = {} } = {}) => {
  const batch = writeBatch(db);
  batch.set(doc(db, 'payments', RESERVATION_ID), validPayment(payment));
  batch.update(doc(db, 'reservations', RESERVATION_ID), {
    status: 'Pago enviado',
    paymentId: RESERVATION_ID,
    ...reservation
  });
  return batch.commit();
};

// Reserva ya en 'Pago enviado' con pago 'Pendiente'.
const seedSubmittedPayment = (paymentOverrides = {}) =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(
      doc(db, 'reservations', RESERVATION_ID),
      validReservation({ status: 'Pago enviado', paymentId: RESERVATION_ID })
    );
    await setDoc(
      doc(db, 'payments', RESERVATION_ID),
      validPayment(paymentOverrides)
    );
  });

// Reserva con pago rechazado (lista para reenvío).
const seedRejectedPayment = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(
      doc(db, 'reservations', RESERVATION_ID),
      validReservation({ status: 'Pago rechazado', paymentId: RESERVATION_ID })
    );
    await setDoc(
      doc(db, 'payments', RESERVATION_ID),
      validPayment({
        status: 'Rechazado',
        rejectionReason: 'El comprobante no coincide con el importe total.',
        reviewedAtMillis: Date.now() - 1000,
        reviewedBy: AGENT_UID
      })
    );
  });

const reviewBatch = (db, uid, { status, rejectionReason = '' }) => {
  const batch = writeBatch(db);
  batch.update(doc(db, 'payments', RESERVATION_ID), {
    status,
    rejectionReason,
    reviewedAtMillis: Date.now(),
    reviewedBy: uid
  });
  batch.update(doc(db, 'reservations', RESERVATION_ID), {
    status: status === 'Aprobado' ? 'Pagado' : 'Pago rechazado'
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

afterAll(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await seedData();
});

describe('Envío del comprobante por el cliente', () => {
  it('permite enviar un comprobante válido y deja la reserva en Pago enviado', async () => {
    await assertSucceeds(submitPaymentBatch(dbAs(CLIENT_UID)));
  });

  it('permite reenviar tras un rechazo con método y voucher nuevos', async () => {
    await seedRejectedPayment();
    const db = dbAs(CLIENT_UID);
    const batch = writeBatch(db);
    batch.update(doc(db, 'payments', RESERVATION_ID), {
      method: 'Plin',
      amount: 1500,
      currency: 'PEN',
      voucherUrl: 'https://firebasestorage.googleapis.com/v0/b/x/voucher2.jpg',
      status: 'Pendiente',
      rejectionReason: '',
      submittedAtMillis: Date.now(),
      reviewedAtMillis: null,
      reviewedBy: ''
    });
    batch.update(doc(db, 'reservations', RESERVATION_ID), { status: 'Pago enviado' });
    await assertSucceeds(batch.commit());
  });

  it('rechaza marcar la reserva como Pagado sin revisión del agente', async () => {
    await assertFails(
      updateDoc(doc(dbAs(CLIENT_UID), 'reservations', RESERVATION_ID), { status: 'Pagado' })
    );
  });

  it('rechaza enviar la reserva a Pago enviado sin crear el pago', async () => {
    await assertFails(
      updateDoc(doc(dbAs(CLIENT_UID), 'reservations', RESERVATION_ID), {
        status: 'Pago enviado'
      })
    );
  });

  it('rechaza un método de pago fuera del catálogo', async () => {
    await assertFails(
      submitPaymentBatch(dbAs(CLIENT_UID), { payment: { method: 'Efectivo' } })
    );
  });

  it('rechaza un importe que no coincide con el total de la reserva', async () => {
    await assertFails(
      submitPaymentBatch(dbAs(CLIENT_UID), { payment: { amount: 1400 } })
    );
  });

  it('rechaza una moneda distinta a la de la reserva', async () => {
    await assertFails(
      submitPaymentBatch(dbAs(CLIENT_UID), { payment: { currency: 'USD' } })
    );
  });

  it('rechaza un pago que reasigna la revisión a otro agente', async () => {
    await assertFails(
      submitPaymentBatch(dbAs(CLIENT_UID), { payment: { agentId: OTHER_AGENT_UID } })
    );
  });

  it('rechaza un pago que apunta a otra cotización distinta a la reserva', async () => {
    await assertFails(
      submitPaymentBatch(dbAs(CLIENT_UID), { payment: { quotationId: 'otra_cotizacion' } })
    );
  });

  it('rechaza reenviar el comprobante con un agente distinto al de la reserva', async () => {
    await seedRejectedPayment();
    const db = dbAs(CLIENT_UID);
    const batch = writeBatch(db);
    batch.update(doc(db, 'payments', RESERVATION_ID), {
      agentId: OTHER_AGENT_UID,
      status: 'Pendiente',
      rejectionReason: '',
      submittedAtMillis: Date.now(),
      reviewedAtMillis: null,
      reviewedBy: ''
    });
    batch.update(doc(db, 'reservations', RESERVATION_ID), { status: 'Pago enviado' });
    await assertFails(batch.commit());
  });
});

describe('Revisión del pago por el agente', () => {
  it('permite al agente asignado aprobar el pago', async () => {
    await seedSubmittedPayment();
    await assertSucceeds(
      reviewBatch(dbAs(AGENT_UID), AGENT_UID, { status: 'Aprobado' })
    );
  });

  it('permite al agente asignado rechazar con un motivo suficiente', async () => {
    await seedSubmittedPayment();
    await assertSucceeds(
      reviewBatch(dbAs(AGENT_UID), AGENT_UID, {
        status: 'Rechazado',
        rejectionReason: 'La imagen del comprobante está ilegible.'
      })
    );
  });

  it('rechaza un motivo de rechazo demasiado corto', async () => {
    await seedSubmittedPayment();
    await assertFails(
      reviewBatch(dbAs(AGENT_UID), AGENT_UID, { status: 'Rechazado', rejectionReason: 'Malo' })
    );
  });

  it('rechaza que otro agente revise un pago que no le pertenece', async () => {
    await seedSubmittedPayment();
    await assertFails(
      reviewBatch(dbAs(OTHER_AGENT_UID), OTHER_AGENT_UID, { status: 'Aprobado' })
    );
  });

  it('rechaza que el cliente se apruebe su propio pago', async () => {
    await seedSubmittedPayment();
    const db = dbAs(CLIENT_UID);
    const batch = writeBatch(db);
    batch.update(doc(db, 'payments', RESERVATION_ID), {
      status: 'Aprobado',
      reviewedAtMillis: Date.now(),
      reviewedBy: CLIENT_UID
    });
    batch.update(doc(db, 'reservations', RESERVATION_ID), { status: 'Pagado' });
    await assertFails(batch.commit());
  });

  it('impide revisar dos veces un pago ya resuelto', async () => {
    await seedSubmittedPayment();
    await assertSucceeds(
      reviewBatch(dbAs(AGENT_UID), AGENT_UID, { status: 'Aprobado' })
    );
    await assertFails(
      reviewBatch(dbAs(AGENT_UID), AGENT_UID, { status: 'Aprobado' })
    );
  });
});

describe('Lectura y borrado de pagos', () => {
  it('el agente asignado puede leer el pago', async () => {
    await seedSubmittedPayment();
    await assertSucceeds(getDoc(doc(dbAs(AGENT_UID), 'payments', RESERVATION_ID)));
  });

  it('otro cliente no puede leer pagos ajenos', async () => {
    await seedSubmittedPayment();
    await assertFails(getDoc(doc(dbAs(OTHER_CLIENT_UID), 'payments', RESERVATION_ID)));
  });

  it('otro agente no puede leer el pago', async () => {
    await seedSubmittedPayment();
    await assertFails(getDoc(doc(dbAs(OTHER_AGENT_UID), 'payments', RESERVATION_ID)));
  });

  it('nadie puede borrar un pago registrado', async () => {
    await seedSubmittedPayment();
    await assertFails(deleteDoc(doc(dbAs(CLIENT_UID), 'payments', RESERVATION_ID)));
    await assertFails(deleteDoc(doc(dbAs(AGENT_UID), 'payments', RESERVATION_ID)));
  });
});
