package co.unicauca.saberpro.frontend.ui;

import java.awt.Color;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Conversiones de los datos del API a texto o color para mostrarlos en pantalla. */
public final class Formats {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Formats() {
    }

    /** Fecha ISO-8601 del API (p. ej. 2026-10-31T21:30:00Z) en la zona horaria local; "" si no viene. */
    public static String dateTime(String isoInstant) {
        if (isoInstant == null || isoInstant.isBlank()) {
            return "";
        }
        try {
            return DATE_TIME.format(Instant.parse(isoInstant).atZone(ZoneId.systemDefault()));
        } catch (DateTimeParseException ex) {
            return isoInstant;
        }
    }

    /** Fecha local sin zona (p. ej. createdAt = 2026-10-31T16:30:12.5) como "2026-10-31 16:30". */
    public static String localDateTime(String isoLocal) {
        if (isoLocal == null || isoLocal.isBlank()) {
            return "";
        }
        try {
            return DATE_TIME.format(LocalDateTime.parse(isoLocal));
        } catch (DateTimeParseException ex) {
            return isoLocal;
        }
    }

    /** Color hexadecimal del API (statusColor, p. ej. "#90CAF9"); gris claro si no viene o es invalido. */
    public static Color color(String hex) {
        if (hex == null || hex.isBlank()) {
            return Color.LIGHT_GRAY;
        }
        try {
            return Color.decode(hex.trim());
        } catch (NumberFormatException ex) {
            return Color.LIGHT_GRAY;
        }
    }
}
