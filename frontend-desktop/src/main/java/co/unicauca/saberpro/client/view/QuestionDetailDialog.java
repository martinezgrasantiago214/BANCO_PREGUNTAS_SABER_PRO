package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.api.dto.ObservationDto;
import co.unicauca.saberpro.client.api.dto.QuestionDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Panel de solo lectura con la pregunta completa (lo usan el autor y el revisor). */
public final class QuestionDetailDialog {

    private QuestionDetailDialog() {
    }

    public static JPanel questionPanel(QuestionDto q, boolean showAnswer) {
        JPanel p = UiTheme.panel(null);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        JLabel state = new JLabel("  " + q.stateLabel() + "  ");
        state.setOpaque(true);
        state.setBackground(UiTheme.color(q.stateColor()));
        p.add(row("Estado", state));
        p.add(row("Contexto", UiTheme.readOnly(q.context())));
        p.add(row("Pregunta directa", UiTheme.readOnly(q.statement())));
        StringBuilder options = new StringBuilder();
        String[] letters = {"A", "B", "C", "D"};
        for (int i = 0; i < q.options().size(); i++) {
            boolean correct = showAnswer && q.options().get(i).equals(q.correctAnswer());
            options.append(letters[Math.min(i, 3)]).append(") ").append(q.options().get(i))
                    .append(correct ? "   ✔ (correcta)" : "").append('\n');
        }
        p.add(row("Opciones", UiTheme.readOnly(options.toString().trim())));
        if (showAnswer) {
            p.add(row("Justificación", UiTheme.readOnly(q.justification())));
        }
        p.add(row("Bibliografía", UiTheme.readOnly(q.bibliography())));
        p.add(row("Clasificación", UiTheme.readOnly("Competencia: " + q.competence() + "\nTema: " + q.topic()
                + "\nSubtema: " + q.subtopic() + "\nDificultad: " + q.difficulty())));
        return p;
    }

    public static JComponent observationsPanel(List<ObservationDto> observations) {
        if (observations.isEmpty()) {
            return new JLabel("  Sin observaciones de los revisores.");
        }
        StringBuilder sb = new StringBuilder();
        for (ObservationDto o : observations) {
            sb.append("[").append(UiTheme.date(o.createdAt())).append("] ").append(o.reviewerLogin()).append(":\n")
                    .append(o.text()).append("\n\n");
        }
        return UiTheme.readOnly(sb.toString().trim());
    }

    public static void show(Component parent, QuestionDto q, List<ObservationDto> observations) {
        JPanel content = UiTheme.panel(new BorderLayout(0, 8));
        content.add(questionPanel(q, true), BorderLayout.CENTER);
        JPanel obs = UiTheme.panel(new BorderLayout());
        obs.setBorder(UiTheme.titled("Observaciones de los revisores (HU05)"));
        obs.add(observationsPanel(observations), BorderLayout.CENTER);
        content.add(obs, BorderLayout.SOUTH);
        JScrollPane scroll = new JScrollPane(content);
        scroll.setPreferredSize(new Dimension(640, 560));
        JOptionPane.showMessageDialog(parent, scroll, "Detalle de la pregunta", JOptionPane.PLAIN_MESSAGE);
    }

    private static JPanel row(String label, JComponent value) {
        JPanel r = UiTheme.panel(new BorderLayout(8, 0));
        JLabel l = new JLabel(label + ":");
        l.setPreferredSize(new Dimension(120, 20));
        l.setVerticalAlignment(SwingConstants.TOP);
        r.add(l, BorderLayout.WEST);
        r.add(value, BorderLayout.CENTER);
        r.setBorder(new EmptyBorder(3, 0, 3, 0));
        return r;
    }
}
