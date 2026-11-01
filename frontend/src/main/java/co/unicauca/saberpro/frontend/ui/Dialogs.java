package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.client.ApiException;

import javax.swing.JOptionPane;
import java.awt.Component;

import java.util.List;

/** Mensajes comunes de las pantallas. */
public final class Dialogs {

    private Dialogs() {
    }

    /**
     * Muestra un error del backend. Si el caso de uso aun no esta implementado
     * (HTTP 501), lo presenta como funcionalidad pendiente con su issue.
     */
    public static void showApiError(Component parent, ApiException ex, String title) {
        if (ex.isPendingFeature()) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Funcionalidad pendiente",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, formatMessages(ex), title, JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Texto del error para el usuario. Si el servidor devolvio varios
     * mensajes (por ejemplo, todas las reglas de la validacion estructural
     * que incumple una pregunta, PSP-17), los muestra como lista.
     */
    public static String formatMessages(ApiException ex) {
        List<String> messages = ex.getMessages();
        if (messages.size() <= 1) {
            return ex.getMessage();
        }
        StringBuilder text = new StringBuilder("Corrija lo siguiente:\n");
        for (String message : messages) {
            text.append("\n  \u2022 ").append(message);
        }
        return text.toString();
    }

    /** Aviso para las opciones de menu cuya pantalla aun no existe. */
    public static void showPendingScreen(Component parent, String feature, String issues) {
        JOptionPane.showMessageDialog(parent, "La pantalla \"" + feature + "\" esta pendiente de implementar ("
                        + issues + ").\nLa fachada (BancoPreguntasFacade) ya tiene las operaciones que necesita.",
                "Funcionalidad pendiente", JOptionPane.INFORMATION_MESSAGE);
    }
}
