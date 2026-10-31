package co.unicauca.saberpro.client.view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Identidad visual y componentes reutilizables (conserva la paleta del primer corte). */
public final class UiTheme {

    public static final Color PRIMARY = new Color(0x1D4E6E);
    public static final Color ACCENT = new Color(0x2E9E8C);
    public static final Color BACKGROUND = new Color(0xF4F6F8);
    public static final Color TEXT = new Color(0x21262B);
    public static final Color DANGER = new Color(0xB23B3B);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private UiTheme() {
    }

    public static JPanel header(String title, String subtitle) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PRIMARY);
        panel.setBorder(new EmptyBorder(12, 18, 12, 18));
        JLabel label = new JLabel(title);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 17f));
        panel.add(label, BorderLayout.WEST);
        if (subtitle != null) {
            JLabel sub = new JLabel(subtitle);
            sub.setForeground(new Color(0xD6E4EE));
            panel.add(sub, BorderLayout.EAST);
        }
        return panel;
    }

    public static JButton primary(String text) {
        return style(new JButton(text), ACCENT);
    }

    public static JButton secondary(String text) {
        return style(new JButton(text), PRIMARY);
    }

    public static JButton danger(String text) {
        return style(new JButton(text), DANGER);
    }

    private static JButton style(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(8, 14, 8, 14));
        return b;
    }

    public static JPanel panel(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(BACKGROUND);
        return p;
    }

    public static JTextArea textArea(int rows) {
        JTextArea area = new JTextArea(rows, 40);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public static JTextArea readOnly(String text) {
        JTextArea area = textArea(2);
        area.setText(text);
        area.setEditable(false);
        area.setBackground(Color.WHITE);
        area.setBorder(new EmptyBorder(4, 6, 4, 6));
        return area;
    }

    public static TitledBorder titled(String title) {
        return BorderFactory.createTitledBorder(title);
    }

    public static Color color(String hex) {
        try {
            return hex == null ? Color.LIGHT_GRAY : Color.decode(hex);
        } catch (NumberFormatException e) {
            return Color.LIGHT_GRAY;
        }
    }

    public static String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant);
    }

    /** HU02: celda con el color del estado. El valor de la celda es un StateCell. */
    public static class StateRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean focus,
                                                       int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            if (value instanceof StateCell s) {
                setText(s.label());
                setHorizontalAlignment(CENTER);
                c.setBackground(sel ? color(s.color()).darker() : color(s.color()));
                c.setForeground(TEXT);
            }
            return c;
        }
    }

    /** Valor de celda de estado: etiqueta + color hexadecimal. */
    public record StateCell(String label, String color) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Leyenda de colores de los estados (HU02). */
    public static JPanel legend(java.util.List<co.unicauca.saberpro.client.api.dto.CatalogItemDto> states) {
        JPanel legend = panel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        legend.add(new JLabel("Estados:"));
        for (var s : states) {
            JLabel chip = new JLabel("  " + s.label() + "  ");
            chip.setOpaque(true);
            chip.setBackground(color(s.color()));
            chip.setBorder(new EmptyBorder(3, 4, 3, 4));
            legend.add(chip);
        }
        return legend;
    }
}
