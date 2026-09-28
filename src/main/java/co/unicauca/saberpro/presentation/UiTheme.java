package co.unicauca.saberpro.presentation;

import javax.swing.*;
import java.awt.*;

/**
 * Paleta de color unica de la aplicacion: solo 4 colores para toda la
 * interfaz (encabezados, botones, fondos y texto), para mantener una
 * identidad visual simple y consistente.
 *
 * Excepcion deliberada: los colores por estado de una pregunta
 * (QuestionState.getColor()) no forman parte de esta paleta, porque HU02
 * exige que cada uno de los 6 estados del ciclo de vida se distinga
 * visualmente con su propio color.
 */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x1D4E6E);     // azul principal (encabezados, marca)
    public static final Color ACCENT = new Color(0x2E9E8C);      // verde-azulado (acciones principales)
    public static final Color BACKGROUND = new Color(0xF4F6F8);  // gris muy claro (fondo)
    public static final Color TEXT = new Color(0x21262B);        // gris oscuro casi negro (texto)

    private UiTheme() {
    }

    /** Barra de titulo superior con el color principal, usada en todas las ventanas. */
    public static JPanel header(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 17f));
        panel.add(label, BorderLayout.WEST);
        return panel;
    }

    /** Boton de accion principal (color de acento). */
    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, ACCENT, Color.WHITE);
        return button;
    }

    /** Boton secundario (mismo esquema, sin protagonismo visual). */
    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        styleButton(button, PRIMARY, Color.WHITE);
        return button;
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    /** Aplica el fondo estandar de la app a un contenedor. */
    public static void applyBackground(JComponent component) {
        component.setBackground(BACKGROUND);
    }
}
