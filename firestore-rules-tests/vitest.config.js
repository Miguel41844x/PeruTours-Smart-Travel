import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    // Los archivos comparten el mismo emulador de Firestore: clearFirestore()
    // de un archivo no debe ejecutarse en paralelo con las pruebas del otro.
    fileParallelism: false
  }
});
