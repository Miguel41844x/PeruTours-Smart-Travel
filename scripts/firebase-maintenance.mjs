import { createRequire } from "node:module";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";
import { fileURLToPath } from "node:url";

const require = createRequire(new URL("../firestore-rules-tests/package.json", import.meta.url));
const cli = "firebase-tools/lib/";
export const root = fileURLToPath(new URL("../", import.meta.url));
export const normalize = (value) => String(value ?? "").normalize("NFD")
  .replace(/\p{M}/gu, "").trim().toLowerCase();
export const slug = (value) => normalize(value).replace(/[^a-z0-9]+/g, "_").replace(/^_|_$/g, "");

export function fromValue(value) {
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("doubleValue" in value) return value.doubleValue;
  if ("booleanValue" in value) return value.booleanValue;
  if ("nullValue" in value) return null;
  if ("timestampValue" in value) return value.timestampValue;
  if ("arrayValue" in value) return (value.arrayValue.values ?? []).map(fromValue);
  if ("mapValue" in value) return fromFields(value.mapValue.fields ?? {});
  throw new Error("Tipo de dato no soportado en los campos de la migración.");
}

export const fromFields = (fields) => Object.fromEntries(
  Object.entries(fields).map(([key, value]) => [key, fromValue(value)])
);

function toValue(value) {
  if (value === null) return { nullValue: null };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(toValue) } };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number" && Number.isFinite(value)) {
    return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  }
  if (typeof value === "object") return { mapValue: { fields: toFields(value) } };
  throw new Error("Valor no soportado en la migración.");
}

const toFields = (record) => Object.fromEntries(
  Object.entries(record).map(([key, value]) => [key, toValue(value)])
);

export async function openDatabase() {
  const { project_info: info } = JSON.parse(readFileSync(resolve(root, "app/google-services.json")));
  const { getProjectDefaultAccount } = require(cli + "auth");
  const { requireAuth } = require(cli + "requireAuth");
  const { Client } = require(cli + "apiv2");
  const authOptions = { project: info.project_id, ...getProjectDefaultAccount(root) };
  await requireAuth(authOptions);
  const api = new Client({ urlPrefix: "https://firestore.googleapis.com", apiVersion: "v1" });
  const database = "projects/" + info.project_id + "/databases/(default)";
  return {
    project: info.project_id,
    async adminServices() {
      const adminRequire = createRequire(new URL("../functions/package.json", import.meta.url));
      const { initializeApp } = adminRequire("firebase-admin/app");
      const { getAuth } = adminRequire("firebase-admin/auth");
      const { getAccessToken } = require(cli + "auth");
      const app = initializeApp({
        projectId: info.project_id, storageBucket: info.storage_bucket,
        credential: {
          async getAccessToken() {
            const token = await getAccessToken(authOptions.tokens?.refresh_token, authOptions.authScopes);
            return {
              access_token: token.access_token,
              expires_in: Math.max(60, Math.floor((token.expires_at - Date.now()) / 1000))
            };
          }
        }
      }, "maintenance-" + Date.now());
      return { auth: getAuth(app) };
    },
    bucketName: info.storage_bucket,
    async ensureDemoImage(objectPath, bytes, downloadToken) {
      const base = "/b/" + info.storage_bucket + "/o";
      const storage = new Client({ urlPrefix: "https://storage.googleapis.com/storage", apiVersion: "v1" });
      const current = await storage.get(base + "/" + encodeURIComponent(objectPath), {
        resolveOnHTTPError: true, skipLog: { resBody: true }
      });
      if (current.status === 200) return current.body.metadata.firebaseStorageDownloadTokens.split(",")[0];
      if (current.status !== 404) throw new Error("No se pudo consultar el comprobante de demostración.");
      const boundary = "perutours_demo_image";
      const metadata = JSON.stringify({
        name: objectPath, contentType: "image/png",
        metadata: { firebaseStorageDownloadTokens: downloadToken }
      });
      const multipart = Buffer.concat([
        Buffer.from("--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n" +
          metadata + "\r\n--" + boundary + "\r\nContent-Type: image/png\r\n\r\n"),
        bytes, Buffer.from("\r\n--" + boundary + "--")
      ]);
      const upload = new Client({ urlPrefix: "https://storage.googleapis.com/upload/storage", apiVersion: "v1" });
      await upload.post(base, multipart, {
        queryParams: { uploadType: "multipart" },
        headers: { "Content-Type": "multipart/related; boundary=" + boundary },
        skipLog: { body: true, resBody: true }
      });
      return downloadToken;
    },
    async list(collection) {
      const documents = [];
      let pageToken;
      do {
        const response = await api.get("/" + database + "/documents/" + collection, {
          queryParams: { pageSize: 300, ...(pageToken ? { pageToken } : {}) },
          skipLog: { resBody: true }
        });
        documents.push(...(response.body.documents ?? []));
        pageToken = response.body.nextPageToken;
      } while (pageToken);
      return documents.map((document) => ({
        id: document.name.split("/").at(-1), document, data: fromFields(document.fields ?? {})
      }));
    },
    async apply(changes, backup, label) {
      const directory = resolve(root, ".artifacts");
      mkdirSync(directory, { recursive: true });
      const filename = label + "-" + new Date().toISOString().replace(/[:.]/g, "-") + ".json";
      writeFileSync(resolve(directory, filename), JSON.stringify({
        project: info.project_id, backup, changes
      }, null, 2));
      console.log("Respaldo local guardado en .artifacts/" + filename);
      if (!process.argv.includes("--apply")) {
        console.log("Vista previa. Agrega --apply para guardar los cambios.");
        return;
      }
      for (let offset = 0; offset < changes.length; offset += 100) {
        const writes = changes.slice(offset, offset + 100).map(({ collection, id, patch, previous }) => ({
          update: {
            name: database + "/documents/" + collection + "/" + id,
            fields: { ...(previous?.fields ?? {}), ...toFields(patch) }
          },
          currentDocument: previous ? { updateTime: previous.updateTime } : { exists: false }
        }));
        await api.post("/" + database + "/documents:commit", { writes }, { skipLog: { resBody: true } });
        console.log("Guardados " + Math.min(offset + 100, changes.length) + " de " + changes.length + ".");
      }
    }
  };
}

export function planPatch(changes, collection, record, patch) {
  const changed = Object.fromEntries(Object.entries(patch).filter(([key, value]) =>
    JSON.stringify(record?.data[key]) !== JSON.stringify(value)
  ));
  if (Object.keys(changed).length > 0) {
    changes.push({ collection, id: record.id, patch: changed, previous: record.document });
    Object.assign(record.data, changed);
  }
}

export function emptyPackageSnapshot() {
  return {
    title: "", summary: "", durationText: "", durationDays: 0,
    priceAmount: 0, currency: "PEN", coverImageUrl: "", conditions: "", includedServiceIds: []
  };
}
