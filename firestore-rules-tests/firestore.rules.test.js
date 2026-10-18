import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails
} from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

let testEnv;

const CLIENT_UID = 'cliente_1';
const CLIENT_EMAIL = 'cliente@perutours.pe';
const AGENT_UID = 'agente_1';
const AGENT_EMAIL = 'agente@perutours.pe';

const clientDb = () =>
  testEnv.authenticatedContext(CLIENT_UID, { email: CLIENT_EMAIL }).firestore();

const otherClientDb = () =>
  testEnv.authenticatedContext('cliente_2', { email: 'otro@perutours.pe' }).firestore();

const agentDb = () =>
  testEnv.authenticatedContext(AGENT_UID, { email: AGENT_EMAIL }).firestore();

// Documento exacto que la app envía en el registro (AuthRepository.buildClientProfileDoc).
const validProfile = (overrides = {}) => ({
  uid: CLIENT_UID,
  name: 'Juan Pérez',
  email: CLIENT_EMAIL,
  phone: '987654321',
  dni: '',
  city: 'Lima, Perú',
  role: 'cliente',
  photoUrl: '',
  preferences: [],
  updatedAt: Date.now(),
  ...overrides
});

const seedUser = (uid, data) =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), 'users', uid), data);
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
});

describe('Registro de usuarios (HU01)', () => {
  it('permite crear un perfil completo con preferencias vacías', async () => {
    await assertSucceeds(setDoc(doc(clientDb(), 'users', CLIENT_UID), validProfile()));
  });

  it('permite crear un perfil con preferencias seleccionadas', async () => {
    await assertSucceeds(
      setDoc(
        doc(clientDb(), 'users', CLIENT_UID),
        validProfile({ preferences: ['aventura', 'gastronomia'] })
      )
    );
  });

  it('rechaza campos adicionales como createdAtMillis', async () => {
    await assertFails(
      setDoc(
        doc(clientDb(), 'users', CLIENT_UID),
        validProfile({ createdAtMillis: Date.now() })
      )
    );
  });

  it('rechaza crear la cuenta con rol agente desde la app pública', async () => {
    await assertFails(
      setDoc(doc(clientDb(), 'users', CLIENT_UID), validProfile({ role: 'agente' }))
    );
  });

  it('rechaza un teléfono que no sea celular peruano', async () => {
    await assertFails(
      setDoc(doc(clientDb(), 'users', CLIENT_UID), validProfile({ phone: '123456789' }))
    );
  });

  it('rechaza un correo que no coincide con el de autenticación', async () => {
    await assertFails(
      setDoc(doc(clientDb(), 'users', CLIENT_UID), validProfile({ email: 'otro@perutours.pe' }))
    );
  });

  it('permite el documento bootstrap mínimo con solo photoUrl', async () => {
    await assertSucceeds(setDoc(doc(clientDb(), 'users', CLIENT_UID), { photoUrl: '' }));
  });
});

describe('Roles y lectura de perfiles', () => {
  it('el dueño puede leer su propio perfil', async () => {
    await seedUser(CLIENT_UID, validProfile());
    await assertSucceeds(getDoc(doc(clientDb(), 'users', CLIENT_UID)));
  });

  it('otro cliente no puede leer un perfil ajeno', async () => {
    await seedUser(CLIENT_UID, validProfile());
    await assertFails(getDoc(doc(otherClientDb(), 'users', CLIENT_UID)));
  });

  it('un agente sí puede leer perfiles para preparar cotizaciones', async () => {
    await seedUser(CLIENT_UID, validProfile());
    await seedUser(
      AGENT_UID,
      validProfile({ uid: AGENT_UID, email: AGENT_EMAIL, role: 'agente' })
    );
    await assertSucceeds(getDoc(doc(agentDb(), 'users', CLIENT_UID)));
  });

  it('un cliente no puede ascenderse a agente', async () => {
    await seedUser(CLIENT_UID, validProfile());
    await assertFails(
      updateDoc(doc(clientDb(), 'users', CLIENT_UID), {
        role: 'agente',
        updatedAt: Date.now()
      })
    );
  });

  it('el dueño puede actualizar su perfil completo', async () => {
    await seedUser(CLIENT_UID, validProfile());
    await assertSucceeds(
      updateDoc(doc(clientDb(), 'users', CLIENT_UID), {
        preferences: ['aventura'],
        updatedAt: Date.now()
      })
    );
  });
});
