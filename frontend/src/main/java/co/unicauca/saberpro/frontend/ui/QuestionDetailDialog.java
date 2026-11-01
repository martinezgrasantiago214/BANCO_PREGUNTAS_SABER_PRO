package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.model.Models.QuestionView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * HU05 (PSP-46): todo el contenido de una pregunta en modo solo lectura, para
 * que el revisor la lea antes de evaluarla. Usa las mismas cuatro secciones
 * que el formulario de creacion, asi el revisor ve la pregunta como la
 * escribio el autor.
 */
public class QuestionDetailDialog extends JDialog {

    protected final QuestionView question;
    private final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));

    public QuestionDetailDialog(Window owner, QuestionView question) {
        super(owner, "Detalle de la pregunta", ModalityType.APPLICATION_MODAL);
        this.question = question;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(700, 760);
        setMinimumSize(new Dimension(520, 480));
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Detalle de la pregunta"), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(12, 16, 12, 16));
        UiTheme.applyBackground(content);

        content.add(statusLine());
        content.add(Box.createVerticalStrut(8));

        JPanel statement = section("1. Enunciado");
        statement.add(labeled("Contexto", readOnly(question.context())));
        statement.add(labeled("Pregunta directa", readOnly(question.directQuestion())));
        addSection(content, statement);

        JPanel options = section("2. Opciones de respuesta");
        List<String> distractors = question.distractors() != null ? question.distractors() : List.of();
        for (int i = 0; i < distractors.size(); i++) {
            options.add(option((char) ('A' + i), distractors.get(i),
                    distractors.get(i).equals(question.correctAnswer())));
        }
        addSection(content, options);

        JPanel support = section("3. Sustento");
        support.add(labeled("Justificacion de la respuesta", readOnly(question.justification())));
        support.add(labeled("Bibliografia", readOnly(question.bibliography())));
        addSection(content, support);

        JPanel classification = section("4. Clasificacion");
        classification.add(labeled("Competencia", readOnly(question.competence())));
        classification.add(labeled("Tema", readOnly(question.topic())));
        classification.add(labeled("Subtema", readOnly(question.subtopic())));
        classification.add(labeled("Nivel de dificultad", readOnly(
                question.difficultyLevel() != null ? question.difficultyLevel().toString() : "")));
        addSection(content, classification);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scroll, BorderLayout.CENTER);

        UiTheme.applyBackground(buttonBar);
        buttonBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.PRIMARY));
        JButton btnClose = UiTheme.secondaryButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        buttonBar.add(btnClose);
        root.add(buttonBar, BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Agrega un boton a la barra inferior, a la derecha de "Cerrar". */
    public void addAction(JButton button) {
        buttonBar.add(button);
        buttonBar.revalidate();
    }

    /** Estado actual con su color (HU02), autor y fecha de creacion. */
    private JPanel statusLine() {
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line.setOpaque(false);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel status = new JLabel(" " + question.statusLabel() + " ");
        status.setOpaque(true);
        status.setBackground(Formats.color(question.statusColor()));
        status.setForeground(UiTheme.TEXT);
        status.setBorder(new EmptyBorder(3, 8, 3, 8));
        status.setFont(status.getFont().deriveFont(Font.BOLD));
        line.add(new JLabel("Estado:"));
        line.add(status);

        JLabel author = new JLabel("Autor: " + question.authorLogin()
                + (question.createdAt() != null ? "   ·   Creada: " + Formats.localDateTime(question.createdAt()) : ""));
        author.setForeground(UiTheme.TEXT);
        line.add(author);
        return line;
    }

    /** Opcion de respuesta; la correcta se resalta para que el revisor la identifique. */
    private JPanel option(char letter, String text, boolean correct) {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(3, 0, 3, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(letter + ")");
        label.setPreferredSize(new Dimension(28, label.getPreferredSize().height));
        panel.add(label, BorderLayout.WEST);

        JTextArea area = readOnly(text);
        panel.add(area, BorderLayout.CENTER);
        if (correct) {
            area.setFont(area.getFont().deriveFont(Font.BOLD));
            JLabel mark = new JLabel("Respuesta correcta");
            mark.setForeground(UiTheme.ACCENT);
            mark.setFont(mark.getFont().deriveFont(Font.BOLD));
            panel.add(mark, BorderLayout.EAST);
        }
        return panel;
    }

    protected static JTextArea readOnly(String text) {
        JTextArea area = new JTextArea(text != null ? text : "");
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(Color.WHITE);
        area.setForeground(UiTheme.TEXT);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD0D5DA)), new EmptyBorder(4, 6, 4, 6)));
        return area;
    }

    protected static JPanel section(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(UiTheme.PRIMARY), title,
                        TitledBorder.LEFT, TitledBorder.TOP, null, UiTheme.PRIMARY),
                new EmptyBorder(4, 8, 8, 8)));
        return panel;
    }

    protected static void addSection(JPanel content, JPanel section) {
        content.add(section);
        content.add(Box.createVerticalStrut(10));
    }

    protected static JPanel labeled(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel(label);
        title.setForeground(UiTheme.TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        panel.add(title, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }
}
