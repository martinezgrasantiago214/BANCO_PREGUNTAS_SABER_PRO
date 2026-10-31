package co.unicauca.saberpro.notifications.testdoubles;

import co.unicauca.saberpro.notifications.application.port.EmailSenderPort;
import co.unicauca.saberpro.notifications.domain.DeliveryStatus;
import co.unicauca.saberpro.notifications.domain.EmailMessage;

import java.util.ArrayList;
import java.util.List;

public class RecordingEmailSender implements EmailSenderPort {

    public final List<EmailMessage> sent = new ArrayList<>();
    public boolean fail;

    @Override
    public DeliveryStatus send(EmailMessage message) {
        if (fail) {
            throw new IllegalStateException("SMTP caído");
        }
        sent.add(message);
        return DeliveryStatus.SIMULADA;
    }
}
