# Pruebas de reglas de Firestore

Pruebas automatizadas de `../firestore.rules` (registro de usuarios y roles)
que se ejecutan contra el emulador de Firestore con
`@firebase/rules-unit-testing`.

## Requisitos

- Node.js 18 o superior
- Java 11 o superior (lo exige el emulador de Firestore)

## Ejecutar

```bash
npm install
npm test
```

El script `npm test` lanza `firebase emulators:exec` con un proyecto ficticio
(`demo-perutours`), por lo que no requiere credenciales de Firebase.
