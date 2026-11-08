// Reproduce EXACTAMENTE el guardado que falla en producción, con el documento
// real, las claims reales del token y las reglas locales (== desplegadas).
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc } from 'firebase/firestore';
import { afterAll, beforeAll, beforeEach, describe, it } from 'vitest';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
let testEnv;

const UID = 'QodlbaychvRhqB2iPUPE0RyXDJT2';
const EMAIL = 'fabianevs123@gmail.com';

// Documento EXACTO de producción (leído vía REST con el token de la app)
const prodDoc = {
  uid: UID,
  name: 'Fabian',
  email: EMAIL,
  phone: '987654321',
  dni: '',
  city: 'Lima, Perú',
  role: 'cliente',
  photoUrl:
    'https://firebasestorage.googleapis.com/v0/b/peru-tours-bc8f8.firebasestorage.app/o/profile_pictures%2FQodlbaychvRhqB2iPUPE0RyXDJT2.jpg?alt=media&token=986e7dd3-9545-43e4-89b3-ab246c40032c',
  preferences: ['5', '6', '4'],
  updatedAt: 1793053257326,
  fcmToken:
    'dw8v6mgMRtKMTPnqRUrHhO:APA91bGy2W_ky00roetv2CWqySDOE50TzERvZiAquI4nZ-3REzalPaJyDe3cCiwQSpwxapE_OijUHi9rG7xxuhtIcbVRuPCZk4FsIaf4b8rCrQt-1hiPTM8',
};

const seedProd = () =>
  testEnv.withSecurityRulesDisabled((ctx) =>
    setDoc(doc(ctx.firestore(), 'users', UID), prodDoc)
  );

const authed = () =>
  testEnv.authenticatedContext(UID, { email: EMAIL }).firestore();

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-perutours',
    firestore: {
      rules: fs.readFileSync(path.join(__dirname, '..', 'firestore.rules'), 'utf8'),
    },
  });
});
afterAll(() => testEnv.cleanup());
beforeEach(() => testEnv.clearFirestore());

describe('reproducción guardado HU10 (doc real, claims reales)', () => {
  it('0. seed + lectura funciona', async () => {
    await seedProd();
    await assertSucceeds(getDoc(doc(authed(), 'users', UID)));
  });

  it('1. escritura FCM exacta: set merge {fcmToken, updatedAt=ahora}', async () => {
    await seedProd();
    await assertSucceeds(
      setDoc(
        doc(authed(), 'users', UID),
        { fcmToken: 'TESTTOKEN', updatedAt: Date.now() },
        { merge: true }
      )
    );
  });

  it('2. no-op: set merge {photoUrl: mismo valor}', async () => {
    await seedProd();
    await assertSucceeds(
      setDoc(doc(authed(), 'users', UID), { photoUrl: prodDoc.photoUrl }, { merge: true })
    );
  });

  it('3. saveProfile exacto: set merge con TODOS los campos HU10', async () => {
    await seedProd();
    await assertSucceeds(
      setDoc(
        doc(authed(), 'users', UID),
        {
          uid: UID,
          name: 'Fabian',
          email: EMAIL,
          phone: '987654321',
          dni: '',
          city: 'Lima, Perú',
          role: 'cliente',
          photoUrl: prodDoc.photoUrl,
          preferences: ['3'],
          tripType: 'Naturaleza',
          budget: 'Medio',
          duration: '3-5 días',
          updatedAt: Date.now(),
        },
        { merge: true }
      )
    );
  });

  it('4. merge SOLO {updatedAt=ahora} (prueba recency aislada)', async () => {
    await seedProd();
    await assertSucceeds(
      setDoc(doc(authed(), 'users', UID), { updatedAt: Date.now() }, { merge: true })
    );
  });

  it('5. set SIN merge (documento completo)', async () => {
    await seedProd();
    await assertSucceeds(
      setDoc(doc(authed(), 'users', UID), { ...prodDoc, updatedAt: Date.now() })
    );
  });

  // El error de producción también apuntaba a L1195 (allow create): el create
  // del documento completo debe evaluar dentro del presupuesto de 1000
  // expresiones sin seed previo (doc inexistente → operación create).
  it('6. create del documento completo (sin seed, evaluación real de L1195)', async () => {
    await assertSucceeds(
      setDoc(doc(authed(), 'users', UID), { ...prodDoc, updatedAt: Date.now() })
    );
  });

  it('7. create con campos HU10 (tripType/budget/duration)', async () => {
    await assertSucceeds(
      setDoc(doc(authed(), 'users', UID), {
        ...prodDoc,
        tripType: 'Naturaleza',
        budget: 'Medio',
        duration: '3-5 días',
        updatedAt: Date.now(),
      })
    );
  });
});
