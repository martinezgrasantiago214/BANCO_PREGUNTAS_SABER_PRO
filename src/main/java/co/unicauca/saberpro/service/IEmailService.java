package co.unicauca.saberpro.service;

/**
 * Abstraccion para el envio de notificaciones por correo (HU04: "Una vez
 * asignados los revisores, el sistema enviara un email para ser
 * notificados"). Permite cambiar de implementacion (SMTP real, consola,
 * archivo de bandeja de salida) sin tocar QuestionService (DIP).
 */
public interface IEmailService {

    void sendReviewAssignmentNotification(String toEmail, String reviewerName,
                                           String questionId, String questionTitle);
}
