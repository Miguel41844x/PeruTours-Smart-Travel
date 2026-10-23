import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where
} from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const CLIENT_UID = 'trip_client_1';
const OTHER_CLIENT_UID = 'trip_client_2';
const AGENT_UID = 'trip_agent_1';
const OTHER_AGENT_UID = 'trip_agent_2';
const RESERVATION_ID = 'paid_reservation_1';

let testEnv;

const dbFor = (uid) => testEnv.authenticatedContext(uid).firestore();

const profile = (uid, role) => ({
  uid,
  name: uid,
  email: `${uid}@perutours.pe`,
  phone: '987654321',
  dni: '',
  city: 'Lima, Perú',
  role,
  photoUrl: '',
  preferences: [],
  updatedAt: Date.now()
});

const itinerary = (overrides = {}) => {
  const now = Date.now();
  return {
    id: 'activity_1',
    reservationId: RESERVATION_ID,
    clientId: CLIENT_UID,
    agentId: AGENT_UID,
    title: 'Visita guiada a Sacsayhuamán',
    description: 'Recorrido acompañado por un guía local.',
    category: 'Actividad',
    locationName: 'Sacsayhuamán, Cusco',
    latitude: -13.5094,
    longitude: -71.9823,
    startAtMillis: now + 86400000,
    endAtMillis: now + 90000000,
    dayIndex: 1,
    sortOrder: 100,
    providerName: 'Operador Cusco',
    status: 'Programado',
    notes: '',
    source: 'agent',
    createdAtMillis: now,
    updatedAtMillis: now,
    ...overrides
  };
};

const checklist = (overrides = {}) => {
  const now = Date.now();
  return {
    id: 'check_1',
    reservationId: RESERVATION_ID,
    clientId: CLIENT_UID,
    title: 'Documento de identidad',
    category: 'Documentos',
    isCompleted: false,
    createdAtMillis: now,
    updatedAtMillis: now,
    ...overrides
  };
};

const incident = (overrides = {}) => {
  const now = Date.now();
  return {
    id: 'incident_1',
    reservationId: RESERVATION_ID,
    clientId: CLIENT_UID,
    agentId: AGENT_UID,
    category: 'Transporte',
    priority: 'Alta',
    status: 'Abierta',
    description: 'El traslado no llegó al punto de encuentro indicado.',
    locationName: 'Centro de Cusco',
    latitude: -13.5167,
    longitude: -71.9781,
    agentResponse: '',
    createdAtMillis: now,
    updatedAtMillis: now,
    resolvedAtMillis: null,
    ...overrides
  };
};

const seed = async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await Promise.all([
      setDoc(doc(db, 'users', CLIENT_UID), profile(CLIENT_UID, 'cliente')),
      setDoc(doc(db, 'users', OTHER_CLIENT_UID), profile(OTHER_CLIENT_UID, 'cliente')),
      setDoc(doc(db, 'users', AGENT_UID), profile(AGENT_UID, 'agente')),
      setDoc(doc(db, 'users', OTHER_AGENT_UID), profile(OTHER_AGENT_UID, 'agente')),
      setDoc(doc(db, 'reservations', RESERVATION_ID), {
        id: RESERVATION_ID,
        clientId: CLIENT_UID,
        agentId: AGENT_UID,
        status: 'Pagado'
      })
    ]);
  });
};

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8')
    }
  });
});

afterAll(async () => testEnv.cleanup());

beforeEach(async () => {
  await testEnv.clearFirestore();
  await seed();
});

describe('Itinerario del viaje', () => {
  it('permite al agente asignado crear y consultar actividades', async () => {
    const agentDb = dbFor(AGENT_UID);
    await assertSucceeds(
      setDoc(doc(agentDb, 'trip_itinerary_items', 'activity_1'), itinerary())
    );
    await assertSucceeds(
      getDocs(
        query(
          collection(agentDb, 'trip_itinerary_items'),
          where('reservationId', '==', RESERVATION_ID),
          where('agentId', '==', AGENT_UID)
        )
      )
    );
  });

  it('permite al cliente consultar el itinerario de su reserva', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'trip_itinerary_items', 'activity_1'),
        itinerary()
      );
    });
    const clientDb = dbFor(CLIENT_UID);
    await assertSucceeds(
      getDocs(
        query(
          collection(clientDb, 'trip_itinerary_items'),
          where('reservationId', '==', RESERVATION_ID),
          where('clientId', '==', CLIENT_UID)
        )
      )
    );
  });

  it('rechaza escritura del cliente y del agente no asignado', async () => {
    await assertFails(
      setDoc(doc(dbFor(CLIENT_UID), 'trip_itinerary_items', 'activity_1'), itinerary())
    );
    await assertFails(
      setDoc(
        doc(dbFor(OTHER_AGENT_UID), 'trip_itinerary_items', 'activity_1'),
        itinerary({ agentId: OTHER_AGENT_UID })
      )
    );
  });

  it('rechaza contaminación de esquema y cambio de propietario', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'trip_itinerary_items', 'activity_1'),
        itinerary()
      );
    });
    await assertFails(
      updateDoc(doc(dbFor(AGENT_UID), 'trip_itinerary_items', 'activity_1'), {
        injected: 'malicious'
      })
    );
    await assertFails(
      updateDoc(doc(dbFor(AGENT_UID), 'trip_itinerary_items', 'activity_1'), {
        clientId: OTHER_CLIENT_UID
      })
    );
  });
});

describe('Checklist privado del viajero', () => {
  it('permite al cliente crear, completar y eliminar sus pendientes', async () => {
    const clientDb = dbFor(CLIENT_UID);
    const reference = doc(clientDb, 'trip_checklist_items', 'check_1');
    await assertSucceeds(setDoc(reference, checklist()));
    await assertSucceeds(
      updateDoc(reference, { isCompleted: true, updatedAtMillis: Date.now() })
    );
    await assertSucceeds(deleteDoc(reference));
  });

  it('mantiene el checklist oculto para otros clientes y agentes', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'trip_checklist_items', 'check_1'),
        checklist()
      );
    });
    await assertFails(getDoc(doc(dbFor(OTHER_CLIENT_UID), 'trip_checklist_items', 'check_1')));
    await assertFails(getDoc(doc(dbFor(AGENT_UID), 'trip_checklist_items', 'check_1')));
  });

  it('rechaza apropiación, campos adicionales y listas para reservas ajenas', async () => {
    await assertFails(
      setDoc(
        doc(dbFor(OTHER_CLIENT_UID), 'trip_checklist_items', 'check_1'),
        checklist({ clientId: OTHER_CLIENT_UID })
      )
    );
    await assertFails(
      setDoc(
        doc(dbFor(CLIENT_UID), 'trip_checklist_items', 'check_1'),
        checklist({ extraData: 'x' })
      )
    );
  });
});

describe('Incidencias del viaje', () => {
  it('permite al cliente crear y leer su incidencia', async () => {
    const clientDb = dbFor(CLIENT_UID);
    await assertSucceeds(
      setDoc(doc(clientDb, 'trip_incidents', 'incident_1'), incident())
    );
    await assertSucceeds(getDoc(doc(clientDb, 'trip_incidents', 'incident_1')));
  });

  it('permite al agente asignado responder sin cambiar la descripción', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'trip_incidents', 'incident_1'), incident());
    });
    await assertSucceeds(
      updateDoc(doc(dbFor(AGENT_UID), 'trip_incidents', 'incident_1'), {
        agentResponse: 'Contactamos al operador y el vehículo está en camino.',
        status: 'En revisión',
        updatedAtMillis: Date.now(),
        resolvedAtMillis: null
      })
    );
    await assertFails(
      updateDoc(doc(dbFor(AGENT_UID), 'trip_incidents', 'incident_1'), {
        description: 'Descripción alterada',
        agentResponse: 'Cambio indebido',
        status: 'Resuelta',
        updatedAtMillis: Date.now(),
        resolvedAtMillis: Date.now()
      })
    );
  });

  it('rechaza lectura ajena, respuesta del cliente y agente no asignado', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'trip_incidents', 'incident_1'), incident());
    });
    await assertFails(getDoc(doc(dbFor(OTHER_CLIENT_UID), 'trip_incidents', 'incident_1')));
    await assertFails(
      updateDoc(doc(dbFor(CLIENT_UID), 'trip_incidents', 'incident_1'), {
        agentResponse: 'Respuesta falsa',
        status: 'Resuelta',
        updatedAtMillis: Date.now(),
        resolvedAtMillis: Date.now()
      })
    );
    await assertFails(
      updateDoc(doc(dbFor(OTHER_AGENT_UID), 'trip_incidents', 'incident_1'), {
        agentResponse: 'No asignado',
        status: 'En revisión',
        updatedAtMillis: Date.now(),
        resolvedAtMillis: null
      })
    );
  });
});
