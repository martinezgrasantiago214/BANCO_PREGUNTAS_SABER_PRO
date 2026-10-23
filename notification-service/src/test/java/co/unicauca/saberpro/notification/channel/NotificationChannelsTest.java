package co.unicauca.saberpro.notification.channel;

import co.unicauca.saberpro.notification.domain.DeliveryResult;
import co.unicauca.saberpro.notification.domain.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Patron Adapter (SMTP) y canal de respaldo en archivo. */
class NotificationChannelsTest {

    private final Notification notification =
            new Notification("rev1@unicauca.edu.co", "Revisor Uno", "Asunto", "Cuerpo del mensaje");

    /** Cliente SMTP falso: simula el resultado sin abrir conexiones. */
    private static final class FakeSmtpClient extends SmtpMailClient {
        private final boolean configured;
        private final String error;
        String lastTo;

        FakeSmtpClient(boolean configured, String error) {
            super(new Properties());
            this.configured = configured;
            this.error = error;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public synchronized String send(String toEmail, String subject, String body) {
            lastTo = toEmail;
            return error;
        }
    }

    @Test
    void elAdaptadorTraduceUnEnvioExitosoDelClienteSmtp() {
        FakeSmtpClient client = new FakeSmtpClient(true, null);
        DeliveryResult result = new SmtpEmailChannelAdapter(client).send(notification);

        assertTrue(result.delivered());
        assertEquals(SmtpEmailChannelAdapter.NAME, result.channel());
        assertEquals("rev1@unicauca.edu.co", client.lastTo);
    }

    @Test
    void elAdaptadorTraduceElErrorDelClienteSmtp() {
        DeliveryResult result = new SmtpEmailChannelAdapter(new FakeSmtpClient(true, "timeout")).send(notification);

        assertFalse(result.delivered());
        assertEquals("timeout", result.detail());
    }

    @Test
    void elAdaptadorNoIntentaEnviarSinConfiguracionOSinCorreo() {
        FakeSmtpClient client = new FakeSmtpClient(false, null);
        assertFalse(new SmtpEmailChannelAdapter(client).send(notification).delivered());
        assertNull(client.lastTo);

        Notification sinCorreo = new Notification(" ", "Revisor", "Asunto", "Cuerpo");
        assertFalse(new SmtpEmailChannelAdapter(new FakeSmtpClient(true, null)).send(sinCorreo).delivered());
    }

    @Test
    void elClienteSmtpSinHostNoEstaConfigurado() {
        assertFalse(new SmtpMailClient(new Properties()).isConfigured());
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "smtp.gmail.com");
        assertTrue(new SmtpMailClient(props).isConfigured());
    }

    @Test
    void elCanalDeRespaldoGuardaLaNotificacionEnUnArchivo(@TempDir Path tempDir) throws Exception {
        DeliveryResult result = new OutboxFileChannel(tempDir.toFile()).send(notification);

        assertTrue(result.delivered());
        File[] files = tempDir.toFile().listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
        String content = Files.readString(files[0].toPath(), StandardCharsets.UTF_8);
        assertTrue(content.contains("rev1@unicauca.edu.co"));
        assertTrue(content.contains("Cuerpo del mensaje"));
    }
}
