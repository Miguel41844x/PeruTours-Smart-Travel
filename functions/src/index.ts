import {setGlobalOptions} from "firebase-functions";
import {
  onDocumentCreated,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";
import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";

setGlobalOptions({maxInstances: 10});

initializeApp();

const db = getFirestore();
const messaging = getMessaging();

export const enviarNotificacionCotizacion = onDocumentCreated(
  "cotizaciones/{quotationId}",
  async (event) => {
    const quotation = event.data?.data();

    if (!quotation) {
      return;
    }

    const clientId = quotation.clientId;

    if (!clientId) {
      console.log("La cotización no tiene clientId.");
      return;
    }

    const clientDocument = await db
      .collection("users")
      .doc(clientId)
      .get();

    if (!clientDocument.exists) {
      console.log(`No existe el usuario ${clientId}.`);
      return;
    }

    const clientData = clientDocument.data();

    const fcmToken = clientData?.fcmToken;

    if (!fcmToken) {
      console.log(
        `El cliente ${clientId} no tiene fcmToken.`
      );
      return;
    }

    const destination =
      quotation.destination || "tu viaje";

    const totalAmount =
      quotation.totalAmount ?? 0;

    const quotationId =
      event.params.quotationId;

    await messaging.send({
      token: fcmToken,

      notification: {
        title: "¡Tu cotización está lista! ✈️",
        body:
          "El agente preparó tu propuesta para " +
          `${destination} por $${totalAmount} USD.`,
      },

      data: {
        quotationId: quotationId,
        type: "quotation",
        status: "Cotizado",
      },
    });

    console.log(
      "Notificación enviada correctamente."
    );
  }
);

export const enviarRespuestaCotizacion = onDocumentUpdated(
  "cotizaciones/{quotationId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after || before.status === after.status) {
      return;
    }

    const responseStatuses = ["Aceptada", "Observada", "Cancelada"];
    if (!responseStatuses.includes(after.status) || !after.agentId) {
      return;
    }

    const agentDocument = await db
      .collection("users")
      .doc(after.agentId)
      .get();
    const agentToken = agentDocument.data()?.fcmToken;

    if (!agentToken) {
      console.log(`El agente ${after.agentId} no tiene fcmToken.`);
      return;
    }

    const destination = after.destination || "el viaje solicitado";
    const quotationId = event.params.quotationId;

    await messaging.send({
      token: agentToken,
      notification: {
        title: "Respuesta a una cotización",
        body:
          `El cliente marcó como ${after.status} ` +
          `la propuesta para ${destination}.`,
      },
      data: {
        quotationId: quotationId,
        type: "quotation_response",
        status: after.status,
      },
    });

    console.log(`Respuesta ${after.status} notificada al agente.`);
  }
);
