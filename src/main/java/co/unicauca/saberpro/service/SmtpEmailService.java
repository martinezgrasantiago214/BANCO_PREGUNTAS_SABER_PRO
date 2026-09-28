package co.unicauca.saberpro.service;

import co.unicauca.saberpro.access.AppPaths;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementacion de {@link IEmailService} que envia el correo por SMTP
 * usando un cliente minimo escrito solo con clases del JDK (java.net.Socket
 * y javax.net.ssl), sin depender de ninguna libreria externa. Lee la
 * configuracion desde "mail.properties" en el classpath
 * (src/main/resources/mail.properties).
 *
 * Soporta los dos modos de cifrado que usan los proveedores reales:
 *  - STARTTLS (puerto 587, p. ej. Gmail/Outlook): mail.smtp.starttls.enable=true
 *  - SSL directo (puerto 465):                    mail.smtp.ssl.enable=true
 * y autenticacion AUTH LOGIN (en Gmail se usa una "contrasena de aplicacion").
 *
 * Si el archivo no existe, la conexion SMTP falla, o cualquier otro error
 * ocurre (por ejemplo, no hay salida a Internet en el entorno de
 * evaluacion), la notificacion igual se registra en la bandeja de salida
 * local (~/BancoPreguntasSaberPro/outbox) y en consola, de forma que HU04
 * sea verificable sin depender de un servidor SMTP real.
 */
public class SmtpEmailService implements IEmailService {

    private static final Logger LOGGER = Logger.getLogger(SmtpEmailService.class.getName());
    private static final String CRLF = "\r\n";
    private static final int TIMEOUT_MS = 15000;

    private final Properties smtpConfig;
    private final List<String> deliveryReport = new ArrayList<>();

    public SmtpEmailService() {
        this.smtpConfig = loadConfig();
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible leer mail.properties", ex);
        }
        return props;
    }

    @Override
    public synchronized void sendReviewAssignmentNotification(String toEmail, String reviewerName,
                                                                String questionId, String questionTitle) {
        String subject = "Banco de Preguntas Saber PRO - Nueva pregunta asignada para revisión";
        String body = "Hola " + reviewerName + ",\n\n"
                + "Se te ha asignado la revisión de la siguiente pregunta:\n\n"
                + "  ID: " + questionId + "\n"
                + "  Pregunta: " + questionTitle + "\n\n"
                + "Por favor ingresa al sistema para revisarla.\n\n"
                + "Banco de Preguntas Saber PRO";

        String error = null;
        if (toEmail == null || toEmail.isBlank()) {
            error = "el revisor no tiene correo registrado";
        } else if (!isConfigured()) {
            error = "no hay servidor SMTP configurado (mail.properties)";
        } else {
            error = trySendSmtp(toEmail, subject, body);
        }

        if (error == null) {
            deliveryReport.add("Enviado por correo a " + toEmail);
        } else {
            File file = writeToOutbox(toEmail, subject, body);
            deliveryReport.add("Simulado para " + toEmail + " (" + error + ")"
                    + (file != null ? " -> " + file.getAbsolutePath() : ""));
        }
    }

    @Override
    public synchronized List<String> consumeDeliveryReport() {
        List<String> copy = new ArrayList<>(deliveryReport);
        deliveryReport.clear();
        return copy;
    }

    private boolean isConfigured() {
        String host = smtpConfig.getProperty("mail.smtp.host");
        return host != null && !host.isBlank();
    }

    private boolean flag(String key) {
        return Boolean.parseBoolean(smtpConfig.getProperty(key, "false").trim());
    }

    /**
     * Cliente SMTP minimo: EHLO, [STARTTLS], [AUTH LOGIN], MAIL FROM, RCPT TO,
     * DATA, QUIT.
     *
     * @return null si el correo se envio, o la descripcion del error.
     */
    private String trySendSmtp(String toEmail, String subject, String body) {
        String host = smtpConfig.getProperty("mail.smtp.host").trim();
        int port = Integer.parseInt(smtpConfig.getProperty("mail.smtp.port", "25").trim());
        String user = smtpConfig.getProperty("mail.smtp.user", "").trim();
        String password = smtpConfig.getProperty("mail.smtp.password", "").replace(" ", "");
        String from = smtpConfig.getProperty("mail.smtp.from", user).trim();
        boolean ssl = flag("mail.smtp.ssl.enable") || port == 465;
        boolean startTls = !ssl && flag("mail.smtp.starttls.enable");

        Socket socket = null;
        try {
            if (ssl) {
                socket = SSLSocketFactory.getDefault().createSocket();
            } else {
                socket = new Socket();
            }
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            SmtpConnection smtp = new SmtpConnection(socket);
            smtp.expect(220);
            smtp.command("EHLO bancopreguntas.saberpro", 250);

            if (startTls) {
                smtp.command("STARTTLS", 220);
                SSLSocket tls = (SSLSocket) ((SSLSocketFactory) SSLSocketFactory.getDefault())
                        .createSocket(socket, host, port, true);
                tls.startHandshake();
                socket = tls;
                smtp = new SmtpConnection(socket);
                smtp.command("EHLO bancopreguntas.saberpro", 250);
            }

            if (!user.isBlank()) {
                smtp.command("AUTH LOGIN", 334);
                smtp.command(base64(user), 334);
                smtp.command(base64(password), 235);
            }

            smtp.command("MAIL FROM:<" + from + ">", 250);
            smtp.command("RCPT TO:<" + toEmail + ">", 250, 251);
            smtp.command("DATA", 354);

            StringBuilder message = new StringBuilder();
            message.append("From: ").append(from).append(CRLF);
            message.append("To: ").append(toEmail).append(CRLF);
            message.append("Subject: =?UTF-8?B?").append(base64(subject)).append("?=").append(CRLF);
            message.append("MIME-Version: 1.0").append(CRLF);
            message.append("Content-Type: text/plain; charset=UTF-8").append(CRLF);
            message.append("Content-Transfer-Encoding: 8bit").append(CRLF);
            message.append(CRLF);
            for (String line : body.split("\n", -1)) {
                // "dot-stuffing": una linea que empieza por "." se duplica (RFC 5321)
                message.append(line.startsWith(".") ? "." + line : line).append(CRLF);
            }
            message.append(".").append(CRLF);
            smtp.raw(message.toString());
            smtp.expect(250);

            smtp.command("QUIT", 221);
            LOGGER.info("Correo de notificacion enviado a " + toEmail);
            System.out.println("[EMAIL] Notificacion enviada a " + toEmail);
            return null;
        } catch (Exception ex) {
            String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            LOGGER.log(Level.WARNING, "No fue posible enviar el correo via SMTP, "
                    + "se registrara en la bandeja de salida local: " + reason);
            return "fallo el envio SMTP: " + reason;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // no hay nada mas que hacer
                }
            }
        }
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /** Lectura/escritura de comandos SMTP sobre un socket (plano o cifrado). */
    private static final class SmtpConnection {
        private final BufferedReader in;
        private final Writer out;

        SmtpConnection(Socket socket) throws IOException {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
        }

        void command(String command, int... expectedCodes) throws IOException {
            raw(command + CRLF);
            expect(expectedCodes);
        }

        void raw(String text) throws IOException {
            out.write(text);
            out.flush();
        }

        /**
         * Lee una respuesta completa. Las respuestas SMTP pueden ocupar varias
         * lineas ("250-..." ... "250 ..."); la ultima linea lleva un espacio
         * despues del codigo.
         */
        void expect(int... expectedCodes) throws IOException {
            String line;
            do {
                line = in.readLine();
                if (line == null) {
                    throw new IOException("El servidor SMTP cerro la conexion");
                }
            } while (line.length() > 3 && line.charAt(3) == '-');

            for (int code : expectedCodes) {
                if (line.startsWith(String.valueOf(code))) {
                    return;
                }
            }
            throw new IOException("Respuesta SMTP inesperada: " + line);
        }
    }

    private File writeToOutbox(String toEmail, String subject, String body) {
        try {
            File outboxDir = AppPaths.getOutboxDirectory();
            File file = new File(outboxDir, "email_" + System.currentTimeMillis() + "_"
                    + (toEmail == null ? "sin-correo" : toEmail.replaceAll("[^A-Za-z0-9@._-]", "_")) + ".txt");
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.println("Fecha: " + LocalDateTime.now());
                writer.println("Para: " + toEmail);
                writer.println("Asunto: " + subject);
                writer.println();
                writer.println(body);
            }
            System.out.println("[EMAIL SIMULADO] Notificacion registrada en " + file.getAbsolutePath());
            return file;
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No fue posible escribir en la bandeja de salida local", ex);
            return null;
        }
    }
}
