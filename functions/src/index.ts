import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

export const onSosAlert = functions.firestore
    .document("usuarios/{userId}")
    .onUpdate(async (change, context) => {
        const newData = change.after.data();
        const previousData = change.before.data();

        // Verifica se o statusSos mudou para 'ativo'
        if (newData.statusSos === "ativo" && previousData.statusSos !== "ativo") {
            const emailResponsavel = newData.emailResponsavel;
            if (!emailResponsavel) return null;

            // Busca o responsável pelo e-mail
            val responsavelSnapshot = await admin.firestore().collection("usuarios")
                .where("email", "==", emailResponsavel)
                .limit(1)
                .get();

            if (responsavelSnapshot.empty) return null;

            const responsavelData = responsavelSnapshot.docs[0].data();
            const token = responsavelData.fcmToken;

            if (token) {
                const message = {
                    notification: {
                        title: "ALERTA SOS - " + (newData.nome || "Usuário"),
                        body: newData.ultimoAlertaMensagem || "O usuário precisa de ajuda!",
                    },
                    token: token,
                    android: {
                        priority: "high" as const,
                        notification: {
                            channelId: "icare_alerts",
                            sound: "default",
                            clickAction: "FLUTTER_NOTIFICATION_CLICK" // Opcional, ajuda em alguns contextos
                        }
                    }
                };

                try {
                    await admin.messaging().send(message);
                    console.log("Notificação enviada com sucesso");
                } catch (error) {
                    console.error("Erro ao enviar notificação:", error);
                }
            }
        }
        return null;
    });
