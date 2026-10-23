package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.service.OperationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Formulario para que un autor cree una pregunta de seleccion multiple con
 * unica respuesta (HU01). Al guardar, el sistema valida automaticamente que la
 * informacion este completa y sea consistente (ver microkernel/pipeline).
 *
 * Los campos se agrupan en cuatro secciones que siguen el orden del Diseno
 * Centrado en Evidencia: Enunciado, Opciones de respuesta, Sustento y
 * Clasificacion. Los botones de accion quedan en una barra fija inferior
 * para que siempre esten visibles aunque el formulario requiera scroll.
 *
 * El mismo formulario se reutiliza en modo EDICION (HU03: "listar las
 * preguntas que he creado para poder mas adelante verlas y editarlas"): se
 * abre precargado con una pregunta existente en estado Borrador y, al
 * guardar, los cambios pasan por el mismo pipeline de validacion.
 */
public class CreateQuestionFrame extends JFrame {

    private final AppContext context;
    /** Pregunta que se esta editando, o null si se esta creando una nueva. */
    private final Question editing;

    private final JTextArea txtContext = new JTextArea(4, 30);
    private final JTextArea txtDirectQuestion = new JTextArea(2, 30);
    private final JTextField[] txtDistractors = new JTextField[4];
    private final JComboBox<String> cmbCorrectAnswer = new JComboBox<>();
    private final JTextArea txtJustification = new JTextArea(3, 30);
    private final JTextField txtBibliography = new JTextField(30);
    private final JTextField txtCompetence = new JTextField(30);
    private final JTextField txtTopic = new JTextField(30);
    private final JTextField txtSubtopic = new JTextField(30);
    private final JComboBox<DifficultyLevel> cmbDifficulty = new JComboBox<>(DifficultyLevel.values());

    /** Modo creacion (HU01). */
    public CreateQuestionFrame(AppContext context) {
        this(context, null);
    }

    /** Modo edicion (HU03) si {@code editing} no es null. */
    public CreateQuestionFrame(AppContext context, Question editing) {
        super(editing == null ? "Crear pregunta" : "Editar pregunta");
        this.context = context;
        this.editing = editing;
        buildUi();
        if (editing != null) {
            fillForm(editing);
        }
    }

    /** Precarga el formulario con los datos de la pregunta a editar. */
    private void fillForm(Question q) {
        txtContext.setText(q.getContext());
        txtDirectQuestion.setText(q.getDirectQuestion());
        List<String> options = q.getDistractors();
        for (int i = 0; i < txtDistractors.length; i++) {
            txtDistractors[i].setText(i < options.size() ? options.get(i) : "");
        }
        refreshCorrectAnswerOptions();
        cmbCorrectAnswer.setSelectedItem(q.getCorrectAnswer());
        txtJustification.setText(q.getJustification());
        txtBibliography.setText(q.getBibliography());
        txtCompetence.setText(q.getCompetence());
        txtTopic.setText(q.getTopic());
        txtSubtopic.setText(q.getSubtopic());
        if (q.getDifficultyLevel() != null) {
            cmbDifficulty.setSelectedItem(q.getDifficultyLevel());
        }
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(700, 780);
        setMinimumSize(new Dimension(560, 520));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header(editing == null ? "Crear pregunta" : "Editar pregunta"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(12, 16, 12, 16));
        UiTheme.applyBackground(form);

        JLabel requiredHint = new JLabel("Los campos marcados con * son obligatorios.");
        requiredHint.setForeground(UiTheme.TEXT);
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setOpaque(false);
        hintPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintPanel.add(requiredHint, BorderLayout.CENTER);
        form.add(hintPanel);
        form.add(Box.createVerticalStrut(8));

        JPanel statement = section("1. Enunciado");
        statement.add(labeled(required("Contexto"), scroll(txtContext),
                "Situacion o caso que da sentido a la pregunta."));
        statement.add(labeled(required("Pregunta directa"), scroll(txtDirectQuestion),
                "Enunciado que el estudiante debe responder."));
        addSection(form, statement);

        JPanel options = section("2. Opciones de respuesta");
        JPanel distractorsPanel = new JPanel(new GridLayout(4, 1, 4, 4));
        distractorsPanel.setOpaque(false);
        for (int i = 0; i < 4; i++) {
            txtDistractors[i] = new JTextField();
            txtDistractors[i].getDocument().addDocumentListener(new SimpleDocListener(this::refreshCorrectAnswerOptions));
            distractorsPanel.add(labeledInline(required("Opcion " + (char) ('A' + i)), txtDistractors[i]));
        }
        options.add(labeled("Cuatro distractores (uno de ellos debe ser la respuesta correcta):",
                distractorsPanel, null));
        options.add(labeled(required("Respuesta correcta"), cmbCorrectAnswer,
                "Se elige entre las opciones escritas arriba."));
        addSection(form, options);

        JPanel support = section("3. Sustento");
        support.add(labeled(required("Justificacion de la respuesta"), scroll(txtJustification),
                "Explique por que la respuesta correcta lo es y por que las demas no."));
        support.add(labeled(required("Bibliografia"), txtBibliography,
                "Fuente que respalda la respuesta."));
        addSection(form, support);

        JPanel classification = section("4. Clasificacion");
        classification.add(labeled(required("Competencia"), txtCompetence,
                "Ej. Arquitectura de software."));
        classification.add(labeled(required("Tema"), txtTopic, null));
        classification.add(labeled(required("Subtema"), txtSubtopic, null));
        classification.add(labeled(required("Nivel de dificultad"), cmbDifficulty, null));
        addSection(form, classification);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(formScroll, BorderLayout.CENTER);
        root.add(buildButtonBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    /** Barra inferior fija (fuera del scroll) para que Guardar siempre este a la vista. */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        UiTheme.applyBackground(bar);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.PRIMARY));

        JButton btnCancel = UiTheme.secondaryButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        bar.add(btnCancel);

        if (editing == null) {
            JButton btnClear = UiTheme.secondaryButton("Limpiar");
            btnClear.addActionListener(e -> clearForm());
            bar.add(btnClear);
        }

        JButton btnSave = UiTheme.primaryButton(editing == null ? "Guardar pregunta" : "Guardar cambios");
        btnSave.addActionListener(e -> onSave());
        bar.add(btnSave);
        return bar;
    }

    /** Deja el formulario vacio para empezar otra pregunta (solo en modo creacion). */
    private void clearForm() {
        txtContext.setText("");
        txtDirectQuestion.setText("");
        for (JTextField field : txtDistractors) {
            field.setText("");
        }
        txtJustification.setText("");
        txtBibliography.setText("");
        txtCompetence.setText("");
        txtTopic.setText("");
        txtSubtopic.setText("");
        cmbDifficulty.setSelectedIndex(0);
        txtContext.requestFocusInWindow();
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
                editing == null ? MultipleChoiceQuestionPlugin.TYPE : editing.getType(),
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

        OperationResult result = editing == null
                ? context.getQuestionService().crearPregunta(request)
                : context.getQuestionService().actualizarPregunta(editing.getId(), request);
        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this,
                    editing == null
                            ? "Pregunta creada correctamente en estado Borrador."
                            : "Cambios guardados correctamente.",
                    "Exito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, result.getErrorsAsText(),
                    "No fue posible guardar la pregunta", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Bloque con titulo que agrupa campos relacionados del formulario. */
    private JPanel section(String title) {
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

    private void addSection(JPanel form, JPanel section) {
        form.add(section);
        form.add(Box.createVerticalStrut(10));
    }

    private static String required(String label) {
        return label + " *";
    }

    private JPanel labeled(String label, JComponent component, String help) {
        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel title = new JLabel(label);
        title.setForeground(UiTheme.TEXT);
        panel.add(title, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        if (help != null) {
            JLabel helpLabel = new JLabel(help);
            helpLabel.setFont(helpLabel.getFont().deriveFont(Font.ITALIC, 11f));
            helpLabel.setForeground(Color.GRAY);
            panel.add(helpLabel, BorderLayout.SOUTH);
            component.setToolTipText(help);
        }
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel labeledInline(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(6, 2));
        panel.setOpaque(false);
        JLabel title = new JLabel(label);
        title.setPreferredSize(new Dimension(80, title.getPreferredSize().height));
        panel.add(title, BorderLayout.WEST);
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
