import {setGlobalOptions} from "firebase-functions";
import {onDocumentCreated} from "firebase-functions/v2/firestore";
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
