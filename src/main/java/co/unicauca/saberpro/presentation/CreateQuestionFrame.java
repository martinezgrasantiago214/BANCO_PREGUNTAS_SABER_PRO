package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.service.OperationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario para que un autor cree una pregunta de seleccion multiple con
 * unica respuesta. Al guardar, el sistema valida automaticamente que la
 * informacion este completa y sea consistente (ver microkernel/pipeline).
 */
public class CreateQuestionFrame extends JFrame {

    private final AppContext context;

    private final JTextArea txtContext = new JTextArea(3, 30);
    private final JTextArea txtDirectQuestion = new JTextArea(2, 30);
    private final JTextField[] txtDistractors = new JTextField[4];
    private final JComboBox<String> cmbCorrectAnswer = new JComboBox<>();
    private final JTextArea txtJustification = new JTextArea(2, 30);
    private final JTextField txtBibliography = new JTextField(30);
    private final JTextField txtCompetence = new JTextField(30);
    private final JTextField txtTopic = new JTextField(30);
    private final JTextField txtSubtopic = new JTextField(30);
    private final JComboBox<DifficultyLevel> cmbDifficulty = new JComboBox<>(DifficultyLevel.values());

    public CreateQuestionFrame(AppContext context) {
        super("Crear pregunta");
        this.context = context;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 760);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Crear pregunta"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(16, 16, 16, 16));
        UiTheme.applyBackground(form);

        form.add(labeled("Contexto:", scroll(txtContext)));
        form.add(labeled("Pregunta directa:", scroll(txtDirectQuestion)));

        JPanel distractorsPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        distractorsPanel.setBorder(BorderFactory.createTitledBorder("Cuatro distractores (una debe ser la correcta)"));
        for (int i = 0; i < 4; i++) {
            txtDistractors[i] = new JTextField();
            final int idx = i;
            txtDistractors[i].getDocument().addDocumentListener(new SimpleDocListener(this::refreshCorrectAnswerOptions));
            distractorsPanel.add(labeledInline("Opcion " + (char) ('A' + idx) + ":", txtDistractors[i]));
        }
        form.add(distractorsPanel);
        form.add(Box.createVerticalStrut(6));

        form.add(labeled("Respuesta correcta:", cmbCorrectAnswer));
        form.add(labeled("Justificacion de la respuesta:", scroll(txtJustification)));
        form.add(labeled("Bibliografia:", txtBibliography));
        form.add(labeled("Competencia (ej. Arquitectura de software):", txtCompetence));
        form.add(labeled("Tema:", txtTopic));
        form.add(labeled("Subtema:", txtSubtopic));
        form.add(labeled("Nivel de dificultad:", cmbDifficulty));

        JButton btnSave = UiTheme.primaryButton("Guardar pregunta");
        btnSave.addActionListener(e -> onSave());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UiTheme.applyBackground(buttonsPanel);
        buttonsPanel.add(btnSave);
        form.add(Box.createVerticalStrut(10));
        form.add(buttonsPanel);

        root.add(new JScrollPane(form), BorderLayout.CENTER);
        setContentPane(root);
    }

    private void refreshCorrectAnswerOptions() {
        String previous = (String) cmbCorrectAnswer.getSelectedItem();
        cmbCorrectAnswer.removeAllItems();
        for (JTextField field : txtDistractors) {
            String value = field.getText().trim();
            if (!value.isEmpty()) {
                cmbCorrectAnswer.addItem(value);
            }
        }
        if (previous != null) {
            cmbCorrectAnswer.setSelectedItem(previous);
        }
    }

    private void onSave() {
        List<String> distractors = new ArrayList<>();
        for (JTextField field : txtDistractors) {
            distractors.add(field.getText().trim());
        }
        String correctAnswer = (String) cmbCorrectAnswer.getSelectedItem();

        QuestionRequest request = new QuestionRequest(
                MultipleChoiceQuestionPlugin.TYPE,
                txtContext.getText().trim(),
                txtDirectQuestion.getText().trim(),
                distractors,
                correctAnswer,
                txtJustification.getText().trim(),
                txtBibliography.getText().trim(),
                txtCompetence.getText().trim(),
                txtTopic.getText().trim(),
                txtSubtopic.getText().trim(),
                (DifficultyLevel) cmbDifficulty.getSelectedItem(),
                context.getLoggedInUser().getLogin()
        );

        OperationResult result = context.getQuestionService().crearPregunta(request);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    "Pregunta creada correctamente en estado Borrador.",
                    "Exito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, result.getErrorsAsText(),
                    "No fue posible guardar la pregunta", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JPanel labeled(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel labeledInline(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.add(new JLabel(label), BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane scroll(JTextArea area) {
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return new JScrollPane(area);
    }

    /** Adaptador minimo para reaccionar a cambios de texto sin boilerplate. */
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable onChange;

        SimpleDocListener(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
    }
}
