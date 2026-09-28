package co.unicauca.saberpro.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de HU04 (notificacion por correo) en modo simulado: sin
 * mail.properties, cada correo se guarda en la bandeja de salida local.
 * Se redirige user.home a una carpeta temporal para no ensuciar la real.
 */
class SmtpEmailServiceTest {

    private String originalHome;
    private File tempHome;

    @BeforeEach
    void setUp() throws Exception {
        originalHome = System.getProperty("user.home");
        tempHome = Files.createTempDirectory("saberpro-test").toFile();
        System.setProperty("user.home", tempHome.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalHome);
    }

    @Test
    void hu04SinServidorSmtpElCorreoQuedaEnLaBandejaDeSalida() {
        SmtpEmailService service = new SmtpEmailService();

        service.sendReviewAssignmentNotification("rev1@unicauca.edu.co", "Revisor Uno", "id-1", "¿Pregunta?");

        File outbox = new File(tempHome, "BancoPreguntasSaberPro/outbox");
        File[] files = outbox.listFiles();
        assertNotNull(files);
        assertEquals(1, files.length);
    }

    @Test
    void hu04ElReporteIndicaQueElCorreoFueSimuladoYSeVacia() {
        SmtpEmailService service = new SmtpEmailService();
        service.sendReviewAssignmentNotification("rev1@unicauca.edu.co", "Revisor Uno", "id-1", "¿Pregunta?");

        List<String> report = service.consumeDeliveryReport();
        assertEquals(1, report.size());
        assertTrue(report.get(0).startsWith("Simulado para rev1@unicauca.edu.co"));
        assertTrue(service.consumeDeliveryReport().isEmpty());
    }
}
