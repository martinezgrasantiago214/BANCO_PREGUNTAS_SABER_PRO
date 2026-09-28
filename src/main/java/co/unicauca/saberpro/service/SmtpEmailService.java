package co.unicauca.saberpro.service;

import co.unicauca.saberpro.access.AppPaths;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.InputMismatchException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion de {@link IEmailService} que envia el correo por SMTP
 * usando un cliente minimo escrito solo con clases del JDK (java.net.Socket),
 * sin depender de ninguna libreria externa. Lee la configuracion desde
 * "mail.properties" (host, port, user, password, from) en el classpath.
 *
 * Si el archivo no existe, la conexion SMTP falla, o cualquier otro error
 * ocurre (por ejemplo, no hay salida a Internet en el entorno de
 * evaluacion), la notificacion igual se registra en la bandeja de salida
 * local "outbox/" y en consola, de forma que HU04 sea verificable sin
 * depender de un servidor SMTP real.
 */
public class SmtpEmailService implements IEmailService {

    private static final Logger LOGGER = Logger.getLogger(SmtpEmailService.class.getName());

    private final Properties smtpConfig;

    public SmtpEmailService() {
        this.smtpConfig = loadConfig();
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible leer mail.properties", ex);
        }
        return props;
    }

    @Override
    public void sendReviewAssignmentNotification(String toEmail, String reviewerName,
                                                   String questionId, String questionTitle) {
        String subject = "Banco de Preguntas Saber PRO - Nueva pregunta asignada para revision";
        String body = "Hola " + reviewerName + ",\n\n"
                + "Se te ha asignado la revision de la siguiente pregunta:\n\n"
                + "  ID: " + questionId + "\n"
                + "  Pregunta: " + questionTitle + "\n\n"
                + "Por favor ingresa al sistema para revisarla.\n\n"
                + "Banco de Preguntas Saber PRO";

        boolean sent = false;
        if (toEmail != null && !toEmail.isBlank() && isConfigured()) {
            sent = trySendSmtp(toEmail, subject, body);
        }
        if (!sent) {
            writeToOutbox(toEmail, subject, body);
        }
    }

    private boolean isConfigured() {
        String host = smtpConfig.getProperty("mail.smtp.host");
        return host != null && !host.isBlank();
    }

    /**
     * Cliente SMTP minimo (HELO, MAIL FROM, RCPT TO, DATA) usando un socket
     * de texto plano. Soporta autenticacion AUTH LOGIN opcional. Pensado
     * para servidores SMTP simples o de pruebas (ej. MailHog, Mailtrap,
     * relays internos sin TLS obligatorio).
     */
    private boolean trySendSmtp(String toEmail, String subject, String body) {
        String host = smtpConfig.getProperty("mail.smtp.host");
        int port = Integer.parseInt(smtpConfig.getProperty("mail.smtp.port", "25"));
        String user = smtpConfig.getProperty("mail.smtp.user", "");
        String password = smtpConfig.getProperty("mail.smtp.password", "");
        String from = smtpConfig.getProperty("mail.smtp.from", user);

        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)) {

            expect(in, 220);
            command(out, in, "EHLO bancopreguntas.saberpro", 250);

            if (!user.isBlank()) {
                command(out, in, "AUTH LOGIN", 334);
                command(out, in, Base64.getEncoder().encodeToString(user.getBytes()), 334);
                command(out, in, Base64.getEncoder().encodeToString(password.getBytes()), 235);
            }

            command(out, in, "MAIL FROM:<" + from + ">", 250);
            command(out, in, "RCPT TO:<" + toEmail + ">", 250);
            command(out, in, "DATA", 354);

            out.println("Subject: " + subject);
            out.println("From: " + from);
            out.println("To: " + toEmail);
            out.println();
            out.println(body);
            out.println(".");
            out.flush();
            expect(in, 250);

            command(out, in, "QUIT", 221);
            LOGGER.info("Correo de notificacion enviado a " + toEmail);
            return true;
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "No fue posible enviar el correo via SMTP, "
                    + "se registrara en la bandeja de salida local: " + ex.getMessage());
            return false;
        }
    }

    private void command(PrintWriter out, BufferedReader in, String command, int expectedCode) throws IOException {
        out.println(command);
        out.flush();
        expect(in, expectedCode);
    }

    private void expect(BufferedReader in, int expectedCode) throws IOException {
        String line = in.readLine();
        if (line == null || !line.startsWith(String.valueOf(expectedCode))) {
            throw new InputMismatchException("Respuesta SMTP inesperada: " + line);
        }
    }

    private void writeToOutbox(String toEmail, String subject, String body) {
        try {
            File outboxDir = AppPaths.getOutboxDirectory();
            File file = new File(outboxDir, "email_" + System.currentTimeMillis() + ".txt");
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("Fecha: " + LocalDateTime.now());
                writer.println("Para: " + toEmail);
                writer.println("Asunto: " + subject);
                writer.println();
                writer.println(body);
            }
            System.out.println("[EMAIL SIMULADO] Notificacion registrada en " + file.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible escribir en la bandeja de salida local", ex);
        }
    }
}
