package co.unicauca.saberpro.service;

import java.util.Collections;
import java.util.List;

/**
 * Abstraccion para el envio de notificaciones por correo (HU04: "Una vez
 * asignados los revisores, el sistema enviara un email para ser
 * notificados"). Permite cambiar de implementacion (SMTP real, consola,
 * archivo de bandeja de salida) sin tocar QuestionService (DIP).
 */
public interface IEmailService {

    void sendReviewAssignmentNotification(String toEmail, String reviewerName,
                                           String questionId, String questionTitle);

    /**
     * Devuelve (y limpia) el resumen de las notificaciones realizadas desde
     * la ultima llamada, por ejemplo "Enviado a rev1@x.co" o "Simulado ...".
     * La interfaz grafica lo usa para mostrarle al administrador que paso
     * con cada correo. Metodo por defecto: las implementaciones que no
     * lleven registro simplemente devuelven una lista vacia.
     */
    default List<String> consumeDeliveryReport() {
        return Collections.emptyList();
    }
}
