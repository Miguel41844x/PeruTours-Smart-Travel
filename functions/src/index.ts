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
          `${destination} por S/ ${totalAmount}.`,
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

    if (
      before.status === "Observada" &&
      after.status === "Cotizado" &&
      after.clientId
    ) {
      const clientDocument = await db
        .collection("users")
        .doc(after.clientId)
        .get();
      const clientToken = clientDocument.data()?.fcmToken;

      if (!clientToken) {
        console.log(`El cliente ${after.clientId} no tiene fcmToken.`);
        return;
      }

      const destination = after.destination || "el viaje solicitado";
      const quotationId = event.params.quotationId;

      await messaging.send({
        token: clientToken,
        notification: {
          title: "Tu cotización fue actualizada",
          body: `Revisa la nueva propuesta para ${destination}.`,
        },
        data: {
          quotationId: quotationId,
          type: "quotation",
          status: "Cotizado",
        },
      });

      console.log("Cotización revisada notificada al cliente.");
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

// HU07: cuando el agente aprueba o rechaza un comprobante, el cliente
// recibe una notificación que abre su reserva directamente.
export const notificarRevisionPago = onDocumentUpdated(
  "payments/{paymentId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after || before.status === after.status) {
      return;
    }

    if (
      before.status !== "Pendiente" ||
      (after.status !== "Aprobado" && after.status !== "Rechazado")
    ) {
      return;
    }

    const clientId = after.clientId;

    if (!clientId) {
      console.log("El pago no tiene clientId.");
      return;
    }

    const clientDocument = await db
      .collection("users")
      .doc(clientId)
      .get();

    const clientToken = clientDocument.data()?.fcmToken;

    if (!clientToken) {
      console.log(`El cliente ${clientId} no tiene fcmToken.`);
      return;
    }

    const reservationId =
      after.reservationId || event.params.paymentId;
    const amount = after.amount ?? 0;
    // La base admite PEN y USD: el símbolo debe seguir la moneda del pago.
    const symbol = after.currency === "USD" ? "$" : "S/";
    const approved = after.status === "Aprobado";

    await messaging.send({
      token: clientToken,

      notification: {
        title: approved ? "¡Pago aprobado! ✅" : "Pago rechazado",
        body: approved ?
          `Tu pago por ${symbol} ${amount} ` +
          "fue verificado. Tu reserva está lista." :
          after.rejectionReason ||
            "El agente rechazó tu comprobante. Puedes reenviar uno nuevo.",
      },

      data: {
        reservationId: reservationId,
        type: "payment_review",
        status: after.status,
      },
    });

    console.log(`Pago ${after.status} notificado al cliente.`);
  }
);

// Centro de viaje: una incidencia nueva avisa al agente responsable para
// que pueda abrir directamente el seguimiento de esa reserva.
export const notificarNuevaIncidencia = onDocumentCreated(
  "trip_incidents/{incidentId}",
  async (event) => {
    const incident = event.data?.data();

    if (!incident?.agentId || !incident?.reservationId) {
      console.log("La incidencia no tiene agente o reserva.");
      return;
    }

    const agentDocument = await db
      .collection("users")
      .doc(incident.agentId)
      .get();
    const agentToken = agentDocument.data()?.fcmToken;

    if (!agentToken) {
      console.log(`El agente ${incident.agentId} no tiene fcmToken.`);
      return;
    }

    await messaging.send({
      token: agentToken,
      notification: {
        title: `Nueva incidencia ${incident.priority || ""}`.trim(),
        body: incident.description ||
          "Un viajero solicitó ayuda durante su viaje.",
      },
      data: {
        reservationId: incident.reservationId,
        incidentId: event.params.incidentId,
        type: "trip_incident",
        status: incident.status || "Abierta",
      },
    });

    console.log(`Incidencia notificada al agente ${incident.agentId}.`);
  }
);

// Cuando el agente responde o resuelve la incidencia, el cliente recibe la
// actualización y puede abrir el centro de su viaje desde la notificación.
export const notificarRespuestaIncidencia = onDocumentUpdated(
  "trip_incidents/{incidentId}",
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after || !after.clientId || !after.reservationId) {
      return;
    }

    const responseChanged = before.agentResponse !== after.agentResponse;
    const statusChanged = before.status !== after.status;
    if (!responseChanged && !statusChanged) {
      return;
    }

    const clientDocument = await db
      .collection("users")
      .doc(after.clientId)
      .get();
    const clientToken = clientDocument.data()?.fcmToken;

    if (!clientToken) {
      console.log(`El cliente ${after.clientId} no tiene fcmToken.`);
      return;
    }

    const resolved = after.status === "Resuelta";
    await messaging.send({
      token: clientToken,
      notification: {
        title: resolved ?
          "Tu incidencia fue resuelta" :
          "Tu agente respondió la incidencia",
        body: after.agentResponse ||
          `La solicitud ahora está ${after.status || "en revisión"}.`,
      },
      data: {
        reservationId: after.reservationId,
        incidentId: event.params.incidentId,
        type: "trip_incident_response",
        status: after.status || "En revisión",
      },
    });

    console.log(`Respuesta de incidencia notificada a ${after.clientId}.`);
  }
);
