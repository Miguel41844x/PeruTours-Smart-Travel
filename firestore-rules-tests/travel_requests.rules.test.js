import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { doc, setDoc, updateDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let testEnv;

const CLIENT_UID = 'cliente_viaje_1';
const OTHER_CLIENT_UID = 'cliente_viaje_2';
const AGENT_UID = 'agente_viaje_1';
const OTHER_AGENT_UID = 'agente_viaje_2';
const REQUEST_ID = 'req_viaje_1';
const PACKAGE_ID = 'pkg_arequipa_1';
const DESTINATION_ID = 'dest_arequipa_1';
const COLCA_ID = 'svc_colca_full_day';

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

const seedUsers = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const [uid, role] of [
      [CLIENT_UID, 'cliente'],
      [OTHER_CLIENT_UID, 'cliente'],
      [AGENT_UID, 'agente'],
      [OTHER_AGENT_UID, 'agente']
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
  requestType: 'CUSTOM',
  packageId: '',
  destinationId: '',
  packageSnapshot: {
    title: '', summary: '', durationText: '', durationDays: 0,
    priceAmount: 0, currency: 'PEN', coverImageUrl: '', conditions: '',
    includedServiceIds: []
  },
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
  assignedAgentId: '',
  claimedAtMillis: null,
  ...overrides
});

const packageSnapshot = {
  title: 'Arequipa esencial',
  summary: 'Centro histórico y campiña.',
  durationText: '3 días / 2 noches',
  durationDays: 3,
  priceAmount: 890,
  currency: 'PEN',
  coverImageUrl: 'https://images.example.com/arequipa.jpg',
  conditions: 'Sujeto a disponibilidad.',
  includedServiceIds: ['svc_arequipa_1']
};

const seedPublishedPackage = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'destinations', DESTINATION_ID), {
      id: DESTINATION_ID,
      status: 'PUBLICADO'
    });
    await setDoc(doc(db, 'packages', PACKAGE_ID), {
      id: PACKAGE_ID,
      destinationId: DESTINATION_ID,
      status: 'PUBLICADO',
      ...packageSnapshot
    });
  });

const seedActivity = (id, overrides = {}) =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'catalog_services', id), {
      id, destinationId: DESTINATION_ID, name: 'Colca full day',
      active: true, requestable: true, ...overrides
    });
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
  it('un paquete de seis servicios no admite extras que no puedan cotizarse juntos', async () => {
    await seedPublishedPackage();
    await seedActivity(COLCA_ID);
    const fullSnapshot = {
      ...packageSnapshot, includedServiceIds: Array.from({ length: 6 }, (_, i) => 'included_' + i)
    };
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), 'packages', PACKAGE_ID), fullSnapshot);
    });
    const reference = doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID);
    const request = validRequest({
      requestType: 'PACKAGE', packageId: PACKAGE_ID, destinationId: DESTINATION_ID,
      packageSnapshot: fullSnapshot
    });
    await assertFails(setDoc(reference, { ...request, requestedServiceIds: [COLCA_ID] }));
    await assertSucceeds(setDoc(reference, { ...request, requestedServiceIds: [] }));
  });

  it('acepta actividades opcionales vigentes del destino', async () => {
    await seedPublishedPackage();
    const ids = [COLCA_ID, 'svc_sillar', 'svc_campina', 'svc_city', 'svc_museo'];
    await Promise.all(ids.map(id => seedActivity(id)));
    await assertSucceeds(setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
      validRequest({ destinationId: DESTINATION_ID, requestedServiceIds: ids })));
  });

  it.each([
    { destinationId: 'dest_cusco' },
    { active: false },
    { requestable: false }
  ])('rechaza una actividad no elegible (%j)', async (overrides) => {
    await seedPublishedPackage();
    await seedActivity(COLCA_ID, overrides);
    await assertFails(setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
      validRequest({ destinationId: DESTINATION_ID, requestedServiceIds: [COLCA_ID] })));
  });

  it('rechaza actividades repetidas y extras que ya vienen en el paquete', async () => {
    await seedPublishedPackage();
    await seedActivity(COLCA_ID);
    await seedActivity('svc_arequipa_1');
    const reference = doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID);
    await assertFails(setDoc(reference,
      validRequest({ destinationId: DESTINATION_ID, requestedServiceIds: [COLCA_ID, COLCA_ID] })));
    await assertFails(setDoc(reference, validRequest({
      requestType: 'PACKAGE', packageId: PACKAGE_ID, packageSnapshot,
      destinationId: DESTINATION_ID, requestedServiceIds: ['svc_arequipa_1']
    })));
    await assertSucceeds(setDoc(reference, validRequest({
      requestType: 'PACKAGE', packageId: PACKAGE_ID, packageSnapshot,
      destinationId: DESTINATION_ID, requestedServiceIds: [COLCA_ID]
    })));
  });

  it('permite cinco extras con un paquete publicado y protege sus condiciones', async () => {
    await seedPublishedPackage();
    const ids = [COLCA_ID, 'svc_sillar', 'svc_campina', 'svc_city', 'svc_museo'];
    await Promise.all(ids.map(id => seedActivity(id)));
    const reference = doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID);
    const request = validRequest({
      requestType: 'PACKAGE', packageId: PACKAGE_ID, packageSnapshot,
      destinationId: DESTINATION_ID, requestedServiceIds: ids
    });
    await assertFails(setDoc(reference, {
      ...request, packageSnapshot: { ...packageSnapshot, conditions: 'Condiciones alteradas' }
    }));
    await assertSucceeds(setDoc(reference, request));
  });

  it('conserva el envío de solicitudes de versiones anteriores', async () => {
    const { requestType, packageId, destinationId, packageSnapshot, ...legacyRequest } =
      validRequest();
    await assertSucceeds(setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID), legacyRequest));
  });

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

  it('permite que un agente tome una solicitud pendiente', async () => {
    await setDoc(doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID), validRequest());
    await assertSucceeds(
      updateDoc(doc(dbAs(AGENT_UID), 'travel_requests', REQUEST_ID), {
        assignedAgentId: AGENT_UID,
        claimedAtMillis: Date.now()
      })
    );
  });

  it('impide que otro agente robe una solicitud ya asignada', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'travel_requests', REQUEST_ID),
        validRequest({ assignedAgentId: AGENT_UID, claimedAtMillis: Date.now() })
      );
    });
    await assertFails(
      updateDoc(doc(dbAs(OTHER_AGENT_UID), 'travel_requests', REQUEST_ID), {
        assignedAgentId: OTHER_AGENT_UID,
        claimedAtMillis: Date.now()
      })
    );
  });

  it('permite solicitar un paquete publicado conservando su snapshot', async () => {
    await seedPublishedPackage();
    await assertSucceeds(
      setDoc(
        doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
        validRequest({
          requestType: 'PACKAGE',
          packageId: PACKAGE_ID,
          destinationId: DESTINATION_ID,
          packageSnapshot
        })
      )
    );
  });

  it('rechaza que el cliente altere el precio del paquete solicitado', async () => {
    await seedPublishedPackage();
    await assertFails(
      setDoc(
        doc(dbAs(CLIENT_UID), 'travel_requests', REQUEST_ID),
        validRequest({
          requestType: 'PACKAGE',
          packageId: PACKAGE_ID,
          destinationId: DESTINATION_ID,
          packageSnapshot: { ...packageSnapshot, priceAmount: 1 }
        })
      )
    );
  });
});
