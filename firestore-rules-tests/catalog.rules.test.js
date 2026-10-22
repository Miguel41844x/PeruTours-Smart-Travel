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

const ADMIN_UID = 'admin_1';
const CLIENT_UID = 'cliente_1';
const AGENT_UID = 'agente_1';
const MANAGER_UID = 'gerente_1';

const DESTINATION_ID = 'dest_cusco';
const PACKAGE_ID = 'pkg_machu';
const PROVIDER_ID = 'prov_andes';

const dbAs = (uid) => testEnv.authenticatedContext(uid).firestore();

const userDoc = (uid, role) => ({
  uid,
  name: `Usuario ${uid}`,
  email: `${uid}@perutours.pe`,
  phone: '987654321',
  dni: '',
  city: 'Lima, Perú',
  role,
  photoUrl: '',
  preferences: [],
  updatedAt: Date.now()
});

const validDestination = (overrides = {}) => ({
  id: DESTINATION_ID,
  name: 'Cusco',
  department: 'Cusco',
  summary: 'Capital inca del Perú.',
  latitude: -13.53195,
  longitude: -71.96746,
  coverImageUrl: 'https://images.example.com/cusco.jpg',
  categories: ['Historia y Cultura'],
  externalPlaceId: 'place_123',
  status: 'BORRADOR',
  createdBy: ADMIN_UID,
  createdAtMillis: 1_000,
  updatedAtMillis: 1_000,
  ...overrides
});

const validPackage = (overrides = {}) => ({
  id: PACKAGE_ID,
  destinationId: DESTINATION_ID,
  title: 'Machu Picchu Mágico',
  summary: 'Explora la ciudadela inca.',
  durationText: '4 días / 3 noches',
  durationDays: 4,
  priceAmount: 1299,
  currency: 'PEN',
  ratingScore: 4.9,
  category: 'Historia y Cultura',
  coverImageUrl: 'https://images.example.com/machu.jpg',
  conditions: 'No reembolsable',
  includedServiceIds: [],
  status: 'BORRADOR',
  createdBy: ADMIN_UID,
  createdAtMillis: 1_000,
  updatedAtMillis: 1_000,
  ...overrides
});

const validProvider = (overrides = {}) => ({
  id: PROVIDER_ID,
  name: 'Andes Travel',
  contactName: 'María Rojas',
  phone: '987654321',
  email: 'maria@andestravel.pe',
  city: 'Cusco',
  serviceNames: ['Hoteles'],
  active: true,
  createdBy: ADMIN_UID,
  createdAtMillis: 1_000,
  updatedAtMillis: 1_000,
  ...overrides
});

const seedData = () =>
  testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, 'users', ADMIN_UID), userDoc(ADMIN_UID, 'administrador'));
    await setDoc(doc(db, 'users', CLIENT_UID), userDoc(CLIENT_UID, 'cliente'));
    await setDoc(doc(db, 'users', AGENT_UID), userDoc(AGENT_UID, 'agente'));
    await setDoc(doc(db, 'users', MANAGER_UID), userDoc(MANAGER_UID, 'gerente'));
    await setDoc(doc(db, 'destinations', DESTINATION_ID), validDestination());
  });

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'demo-perutours',
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

describe('destinations (catálogo turístico)', () => {
  it('el cliente puede leer destinos publicados', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'destinations', 'dest_publicado'),
        validDestination({ id: 'dest_publicado', status: 'PUBLICADO' })
      );
    });
    await assertSucceeds(
      getDoc(doc(dbAs(CLIENT_UID), 'destinations', 'dest_publicado'))
    );
  });

  it('el cliente no puede leer borradores del catálogo', async () => {
    await assertFails(getDoc(doc(dbAs(CLIENT_UID), 'destinations', DESTINATION_ID)));
  });

  it('el administrador puede leer sus borradores', async () => {
    await assertSucceeds(getDoc(doc(dbAs(ADMIN_UID), 'destinations', DESTINATION_ID)));
  });

  it('el cliente no puede crear destinos', async () => {
    await assertFails(
      setDoc(doc(dbAs(CLIENT_UID), 'destinations', 'dest_nuevo'), validDestination({ id: 'dest_nuevo' }))
    );
  });

  it('el agente y el gerente no pueden gestionar el catálogo', async () => {
    await assertFails(
      setDoc(doc(dbAs(AGENT_UID), 'destinations', 'dest_nuevo'), validDestination({ id: 'dest_nuevo' }))
    );
    await assertFails(
      setDoc(doc(dbAs(MANAGER_UID), 'destinations', 'dest_nuevo'), validDestination({ id: 'dest_nuevo' }))
    );
  });

  it('el administrador puede registrar un destino válido', async () => {
    await assertSucceeds(
      setDoc(doc(dbAs(ADMIN_UID), 'destinations', 'dest_nuevo'), validDestination({ id: 'dest_nuevo' }))
    );
  });

  it('el destino debe respetar el esquema completo', async () => {
    const invalid = validDestination({ id: 'dest_invalido', latitude: 500 });
    await assertFails(setDoc(doc(dbAs(ADMIN_UID), 'destinations', 'dest_invalido'), invalid));
  });

  it('el administrador puede publicar un destino', async () => {
    await assertSucceeds(
      updateDoc(doc(dbAs(ADMIN_UID), 'destinations', DESTINATION_ID), {
        status: 'PUBLICADO',
        updatedAtMillis: 2_000
      })
    );
  });

  it('no se puede reescribir el autor ni la fecha de creación', async () => {
    await assertFails(
      updateDoc(doc(dbAs(ADMIN_UID), 'destinations', DESTINATION_ID), {
        createdBy: CLIENT_UID,
        updatedAtMillis: 2_000
      })
    );
    await assertFails(
      updateDoc(doc(dbAs(ADMIN_UID), 'destinations', DESTINATION_ID), {
        createdAtMillis: 5,
        updatedAtMillis: 2_000
      })
    );
  });

  it('nadie puede borrar destinos del catálogo', async () => {
    const { deleteDoc } = await import('firebase/firestore');
    await assertFails(deleteDoc(doc(dbAs(ADMIN_UID), 'destinations', DESTINATION_ID)));
  });
});

describe('packages (ofertas vendibles)', () => {
  it('el cliente solo ve paquetes publicados', async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(
        doc(context.firestore(), 'packages', 'pkg_publico'),
        validPackage({ id: 'pkg_publico', status: 'PUBLICADO' })
      );
    });
    await assertSucceeds(getDoc(doc(dbAs(CLIENT_UID), 'packages', 'pkg_publico')));
    await assertFails(getDoc(doc(dbAs(CLIENT_UID), 'packages', PACKAGE_ID)));
  });

  it('el administrador crea paquetes solo como borrador', async () => {
    await assertSucceeds(
      setDoc(doc(dbAs(ADMIN_UID), 'packages', PACKAGE_ID), validPackage())
    );
    await assertFails(
      setDoc(
        doc(dbAs(ADMIN_UID), 'packages', 'pkg_publico_directo'),
        validPackage({ id: 'pkg_publico_directo', status: 'PUBLICADO' })
      )
    );
  });

  it('un paquete debe apuntar a un destino existente', async () => {
    await assertFails(
      setDoc(
        doc(dbAs(ADMIN_UID), 'packages', 'pkg_huerfano'),
        validPackage({ id: 'pkg_huerfano', destinationId: 'dest_inexistente' })
      )
    );
  });

  it('el precio y la duración quedan acotados', async () => {
    await assertFails(
      setDoc(
        doc(dbAs(ADMIN_UID), 'packages', 'pkg_caro'),
        validPackage({ id: 'pkg_caro', priceAmount: -5 })
      )
    );
    await assertFails(
      setDoc(
        doc(dbAs(ADMIN_UID), 'packages', 'pkg_largo'),
        validPackage({ id: 'pkg_largo', durationDays: 9_999 })
      )
    );
  });

  it('el administrador puede publicar un paquete', async () => {
    await setDoc(doc(dbAs(ADMIN_UID), 'packages', PACKAGE_ID), validPackage());
    await assertSucceeds(
      updateDoc(doc(dbAs(ADMIN_UID), 'packages', PACKAGE_ID), {
        status: 'PUBLICADO',
        updatedAtMillis: 2_000
      })
    );
  });

  it('los clientes no escriben paquetes', async () => {
    await assertFails(
      setDoc(doc(dbAs(CLIENT_UID), 'packages', 'pkg_cliente'), validPackage({ id: 'pkg_cliente' }))
    );
  });
});

describe('providers (proveedores turísticos)', () => {
  it('cualquier usuario autenticado puede consultar proveedores', async () => {
    await assertSucceeds(getDoc(doc(dbAs(CLIENT_UID), 'providers', PROVIDER_ID)));
  });

  it('el cliente no puede registrar proveedores', async () => {
    await assertFails(
      setDoc(doc(dbAs(CLIENT_UID), 'providers', 'prov_nuevo'), validProvider({ id: 'prov_nuevo' }))
    );
  });

  it('el administrador registra proveedores válidos', async () => {
    await assertSucceeds(setDoc(doc(dbAs(ADMIN_UID), 'providers', PROVIDER_ID), validProvider()));
  });

  it('el correo del proveedor debe ser válido', async () => {
    await assertFails(
      setDoc(
        doc(dbAs(ADMIN_UID), 'providers', 'prov_correo'),
        validProvider({ id: 'prov_correo', email: 'no-es-un-correo' })
      )
    );
  });

  it('el administrador puede desactivar un proveedor', async () => {
    await setDoc(doc(dbAs(ADMIN_UID), 'providers', PROVIDER_ID), validProvider());
    await assertSucceeds(
      updateDoc(doc(dbAs(ADMIN_UID), 'providers', PROVIDER_ID), {
        active: false,
        updatedAtMillis: 2_000
      })
    );
  });
});
