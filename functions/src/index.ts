import {setGlobalOptions} from "firebase-functions";
import {onDocumentUpdated} from "firebase-functions/v2/firestore";
import {initializeApp} from "firebase-admin/app";
import {getFirestore} from "firebase-admin/firestore";
import {getMessaging} from "firebase-admin/messaging";

setGlobalOptions({maxInstances: 10});

initializeApp();

const db = getFirestore();
const messaging = getMessaging();

export const enviarNotificacionCotizacion = onDocumentUpdated(
  "cotizaciones/{quotationId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) {
      return;
    }

    // Solo enviamos la notificación cuando pasa a "Cotizado".
    if (
      before.status === "Cotizado" ||
      after.status !== "Cotizado"
    ) {
      return;
    }

    const clientId = after.clientId;

    if (!clientId) {
      console.log("La cotización no tiene clientId.");
      return;
    }

    // Buscamos el perfil del cliente.
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
      after.destination || "tu viaje";

    const totalAmount =
      after.totalAmount ?? 0;

    const quotationId = event.params.quotationId;

    const message = {
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
    };

    try {
      const response = await messaging.send(message);

      console.log(
        "Notificación enviada correctamente:",
        response
      );
    } catch (error) {
      console.error(
        "Error al enviar la notificación FCM:",
        error
      );
    }
  }
);
