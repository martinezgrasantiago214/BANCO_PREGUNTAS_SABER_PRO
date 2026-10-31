package co.unicauca.saberpro.notifications.infrastructure.email;

import co.unicauca.saberpro.notifications.application.port.EmailSenderPort;
import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.EmailMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Patron Adapter (GoF, estructural). Adapta la API de Spring Mail
 * ({@link JavaMailSender}, el "Adaptee") a la interfaz que espera la
 * aplicacion ({@link EmailSenderPort}, el "Target"). La aplicacion nunca
 * importa clases de javax/jakarta.mail.
 */
public class SmtpEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpEmailSenderAdapter(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public DeliveryStatus send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
        return DeliveryStatus.ENVIADA;
    }
}
