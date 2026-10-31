package co.unicauca.saberpro.notification.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Cliente SMTP minimo escrito solo con clases del JDK (reutilizado del primer
 * corte). Es la clase "adaptada" (Adaptee) del patron Adapter: su interfaz
 * propia es {@link #send(String, String, String)}, que devuelve null si el
 * correo salio o el texto del error.
 *
 * Soporta STARTTLS (puerto 587, p. ej. Gmail/Outlook) y SSL directo (465),
 * con AUTH LOGIN. La configuracion viene de mail.properties.
 */
public class SmtpMailClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpMailClient.class);
    private static final String CRLF = "\r\n";
    private static final int TIMEOUT_MS = 15000;

    private final Properties config;

    public SmtpMailClient(Properties config) {
        this.config = config;
    }

    /** Lee mail.properties del classpath; si no existe, el cliente queda sin configurar. */
    public static SmtpMailClient fromClasspath() {
        Properties props = new Properties();
        try (InputStream in = SmtpMailClient.class.getClassLoader().getResourceAsStream("mail.properties")) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            LOGGER.warn("No fue posible leer mail.properties", ex);
        }
        return new SmtpMailClient(props);
    }

    public boolean isConfigured() {
        String host = config.getProperty("mail.smtp.host");
        return host != null && !host.isBlank();
    }

    private boolean flag(String key) {
        return Boolean.parseBoolean(config.getProperty(key, "false").trim());
    }

    /**
     * EHLO, [STARTTLS], [AUTH LOGIN], MAIL FROM, RCPT TO, DATA, QUIT.
     *
     * @return null si el correo se envio, o la descripcion del error.
     */
    public synchronized String send(String toEmail, String subject, String body) {
        String host = config.getProperty("mail.smtp.host").trim();
        int port = Integer.parseInt(config.getProperty("mail.smtp.port", "25").trim());
        String user = config.getProperty("mail.smtp.user", "").trim();
        String password = config.getProperty("mail.smtp.password", "").replace(" ", "");
        String from = config.getProperty("mail.smtp.from", user).trim();
        boolean ssl = flag("mail.smtp.ssl.enable") || port == 465;
        boolean startTls = !ssl && flag("mail.smtp.starttls.enable");

        Socket socket = null;
        try {
            socket = ssl ? SSLSocketFactory.getDefault().createSocket() : new Socket();
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
            LOGGER.info("Correo de notificacion enviado a {}", toEmail);
            return null;
        } catch (Exception ex) {
            String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            LOGGER.warn("No fue posible enviar el correo via SMTP: {}", reason);
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

        /** Lee una respuesta completa; las de varias lineas usan "250-" hasta la ultima "250 ". */
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
}
