package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionField;
import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.model.Models.QuestionForm;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Formulario para que un autor cree una pregunta de seleccion multiple con
 * unica respuesta (HU01). Al guardar, la pregunta se envia al question-service
 * (via BancoPreguntasFacade y el API Gateway), que aplica la validacion
 * estructural definitiva.
 *
 * Los campos se agrupan en cuatro secciones que siguen el orden del Diseno
 * Centrado en Evidencia: Enunciado, Opciones de respuesta, Sustento y
 * Clasificacion. Cada campo de texto aplica las reglas de
 * {@link QuestionField}: contador de caracteres y mensaje de error en linea
 * bajo el campo, antes de enviar la pregunta al backend. Los botones de accion
 * quedan en una barra fija inferior para que siempre esten visibles aunque el
 * formulario requiera scroll.
 *
 * El mismo formulario se reutiliza en modo EDICION (HU03: "listar las
 * preguntas que he creado para poder mas adelante verlas y editarlas"): se
 * abre precargado con una pregunta existente en estado Borrador y, al
 * guardar, los cambios pasan por la misma validacion estructural.
 */
public class CreateQuestionFrame extends JFrame {

    /** Tipo de pregunta de HU01 (fabrica MultipleChoiceQuestionFactory del question-service). */
    private static final String MULTIPLE_CHOICE = "MULTIPLE_CHOICE";

    private final AppContext context;
    /** Pregunta que se esta editando, o null si se esta creando una nueva. */
    private final QuestionView editing;

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
    /** Campos de texto con sus reglas (QuestionField), en el orden del enum. */
    private final Map<QuestionField, FieldInput> fieldInputs = new EnumMap<>(QuestionField.class);

    /** Modo creacion (HU01). */
    public CreateQuestionFrame(AppContext context) {
        this(context, null);
    }

    /** Modo edicion (HU03) si {@code editing} no es null. */
    public CreateQuestionFrame(AppContext context, QuestionView editing) {
        super(editing == null ? "Crear pregunta" : "Editar pregunta");
        this.context = context;
        this.editing = editing;
        buildUi();
        if (editing != null) {
            fillForm(editing);
        }
    }

    /** Precarga el formulario con los datos de la pregunta a editar. */
    private void fillForm(QuestionView q) {
        txtContext.setText(q.context());
        txtDirectQuestion.setText(q.directQuestion());
        List<String> options = q.distractors() != null ? q.distractors() : List.of();
        for (int i = 0; i < txtDistractors.length; i++) {
            txtDistractors[i].setText(i < options.size() ? options.get(i) : "");
        }
        refreshCorrectAnswerOptions();
        cmbCorrectAnswer.setSelectedItem(q.correctAnswer());
        txtJustification.setText(q.justification());
        txtBibliography.setText(q.bibliography());
        txtCompetence.setText(q.competence());
        txtTopic.setText(q.topic());
        txtSubtopic.setText(q.subtopic());
        if (q.difficultyLevel() != null) {
            cmbDifficulty.setSelectedItem(q.difficultyLevel());
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
        statement.add(field(QuestionField.CONTEXT, txtContext,
                "Situacion o caso que da sentido a la pregunta."));
        statement.add(field(QuestionField.DIRECT_QUESTION, txtDirectQuestion,
                "Enunciado que el estudiante debe responder (minimo "
                        + QuestionField.DIRECT_QUESTION.getMinLength() + " caracteres)."));
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
        support.add(field(QuestionField.JUSTIFICATION, txtJustification,
                "Explique por que la respuesta correcta lo es y por que las demas no."));
        support.add(field(QuestionField.BIBLIOGRAPHY, txtBibliography,
                "Fuente que respalda la respuesta."));
        addSection(form, support);

        JPanel classification = section("4. Clasificacion");
        classification.add(field(QuestionField.COMPETENCE, txtCompetence,
                "Ej. Arquitectura de software."));
        classification.add(field(QuestionField.TOPIC, txtTopic, null));
        classification.add(field(QuestionField.SUBTOPIC, txtSubtopic, null));
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
        for (FieldInput input : fieldInputs.values()) {
            input.clearError();
        }
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

    /**
     * Valida cada campo de texto con su QuestionField y muestra los errores
     * bajo el campo correspondiente. La validacion estructural del
     * question-service sigue siendo la definitiva al guardar.
     *
     * @return true si todos los campos son validos.
     */
    private boolean validateFields() {
        FieldInput firstInvalid = null;
        for (FieldInput input : fieldInputs.values()) {
            if (!input.validateNow() && firstInvalid == null) {
                firstInvalid = input;
            }
        }
        if (firstInvalid != null) {
            firstInvalid.input.requestFocusInWindow();
            firstInvalid.input.scrollRectToVisible(new Rectangle(firstInvalid.input.getSize()));
            return false;
        }
        return true;
    }

    private void onSave() {
        if (!validateFields()) {
            return;
        }
        List<String> distractors = new ArrayList<>();
        for (JTextField field : txtDistractors) {
            distractors.add(field.getText().trim());
        }
        String correctAnswer = (String) cmbCorrectAnswer.getSelectedItem();

        // El autor lo completa la fachada con el usuario autenticado.
        QuestionForm form = new QuestionForm(
                editing == null ? MULTIPLE_CHOICE : editing.type(),
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
                null
        );

        try {
            if (editing == null) {
                context.getFacade().createQuestion(form);
            } else {
                context.getFacade().updateQuestion(editing.id(), form);
            }
        } catch (ApiException ex) {
            Dialogs.showApiError(this, ex, "No fue posible guardar la pregunta");
            return;
        }
        JOptionPane.showMessageDialog(this,
                editing == null
                        ? "Pregunta creada correctamente en estado Borrador."
                        : "Cambios guardados correctamente.",
                "Exito", JOptionPane.INFORMATION_MESSAGE);
        dispose();
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

    /**
     * Construye un campo de texto a partir de su QuestionField: titulo,
     * componente, linea de ayuda/error y contador de caracteres.
     */
    private JPanel field(QuestionField field, JTextComponent input, String help) {
        JComponent component = input instanceof JTextArea ? scroll((JTextArea) input) : input;
        FieldInput fieldInput = new FieldInput(field, input, help);
        fieldInputs.put(field, fieldInput);

        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel title = new JLabel(required(field.getLabel()));
        title.setForeground(UiTheme.TEXT);
        title.setLabelFor(input);
        panel.add(title, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.add(fieldInput.message, BorderLayout.CENTER);
        footer.add(fieldInput.counter, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);
        if (help != null) {
            input.setToolTipText(help);
        }
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
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

    /**
     * Estado visual de un campo de texto: muestra la ayuda o el error bajo el
     * campo y un contador "n/max" que se pone en rojo al pasarse del limite.
     * No se trunca lo escrito (una pregunta antigua podria superar el limite
     * y se perderia texto al editarla); el exceso se reporta como error.
     * Una vez marcado con error, el campo se revalida mientras se escribe
     * para que el mensaje desaparezca apenas se corrija.
     */
    private static class FieldInput {
        private static final Color HELP_COLOR = Color.GRAY;

        private final QuestionField field;
        private final JTextComponent input;
        private final String help;
        private final javax.swing.border.Border normalBorder;
        private final JLabel message = new JLabel(" ");
        private final JLabel counter = new JLabel();
        private boolean showingError;

        FieldInput(QuestionField field, JTextComponent input, String help) {
            this.field = field;
            this.input = input;
            this.help = help;
            this.normalBorder = input.getBorder();
            message.setFont(message.getFont().deriveFont(Font.ITALIC, 11f));
            counter.setFont(counter.getFont().deriveFont(11f));
            clearError();
            input.getDocument().addDocumentListener(new SimpleDocListener(this::onChange));
        }

        private void onChange() {
            updateCounter();
            if (showingError) {
                validateNow();
            }
        }

        private void updateCounter() {
            int length = input.getText().trim().length();
            counter.setText(length + "/" + field.getMaxLength());
            counter.setForeground(length > field.getMaxLength() ? UiTheme.ERROR : HELP_COLOR);
        }

        boolean validateNow() {
            String error = field.validate(input.getText());
            if (error == null) {
                clearError();
                return true;
            }
            showingError = true;
            message.setText(error);
            message.setForeground(UiTheme.ERROR);
            input.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiTheme.ERROR), new EmptyBorder(1, 2, 1, 2)));
            return false;
        }

        void clearError() {
            showingError = false;
            message.setText(help == null ? " " : help);
            message.setForeground(HELP_COLOR);
            input.setBorder(normalBorder);
            updateCounter();
        }
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
