package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.QuestionDto;
import co.unicauca.saberpro.client.api.dto.QuestionRequestDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HU01 (crear) y HU03 (editar). La respuesta correcta se marca con un
 * boton de radio junto a la opcion, asi es imposible que no coincida con
 * ninguna. La validacion estructural completa la hace question-service y
 * sus errores se muestran todos juntos.
 */
public class QuestionFormView extends JFrame {

    private static final String[] LETTERS = {"A", "B", "C", "D"};
    private final DesktopApp app;
    private final QuestionDto editing;
    private final JTextArea txtContext = UiTheme.textArea(4);
    private final JTextArea txtStatement = UiTheme.textArea(2);
    private final List<JTextField> txtOptions = new ArrayList<>();
    private final List<JRadioButton> rbCorrect = new ArrayList<>();
    private final JTextArea txtJustification = UiTheme.textArea(3);
    private final JTextArea txtBibliography = UiTheme.textArea(2);
    private final JTextField txtCompetence = new JTextField(28);
    private final JTextField txtTopic = new JTextField(28);
    private final JTextField txtSubtopic = new JTextField(28);
    private final JComboBox<String> cmbDifficulty = new JComboBox<>(new String[]{"BAJO", "MEDIO", "ALTO"});

    public QuestionFormView(DesktopApp app, QuestionDto editing) {
        super(editing == null ? "Crear pregunta" : "Editar pregunta");
        this.app = app;
        this.editing = editing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header(editing == null ? "Nueva pregunta de selección múltiple con única respuesta"
                : "Editar pregunta", "Diseño Centrado en Evidencia"), BorderLayout.NORTH);

        JPanel form = UiTheme.panel(null);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(10, 14, 10, 14));
        form.add(section("Contexto", new JScrollPane(txtContext)));
        form.add(section("Pregunta directa", new JScrollPane(txtStatement)));
        form.add(section("Opciones de respuesta (marque la correcta)", optionsPanel()));
        form.add(section("Justificación de la respuesta", new JScrollPane(txtJustification)));
        form.add(section("Bibliografía", new JScrollPane(txtBibliography)));
        form.add(section("Clasificación", classificationPanel()));
        root.add(new JScrollPane(form), BorderLayout.CENTER);

        JPanel actions = UiTheme.panel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = UiTheme.secondary("Cancelar");
        cancel.addActionListener(e -> dispose());
        JButton save = UiTheme.primary(editing == null ? "Guardar como borrador" : "Guardar cambios");
        save.addActionListener(e -> save());
        actions.add(cancel);
        actions.add(save);
        root.add(actions, BorderLayout.SOUTH);

        if (editing != null) {
            fill(editing);
        }
        setContentPane(root);
        setSize(760, 760);
        setLocationRelativeTo(null);
    }

    private JPanel section(String title, JComponent content) {
        JPanel p = UiTheme.panel(new BorderLayout());
        p.setBorder(UiTheme.titled(title));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel optionsPanel() {
        JPanel p = UiTheme.panel(new GridLayout(4, 1, 4, 4));
        ButtonGroup group = new ButtonGroup();
        for (String letter : LETTERS) {
            JRadioButton rb = new JRadioButton("Correcta");
            rb.setBackground(UiTheme.BACKGROUND);
            group.add(rb);
            JTextField tf = new JTextField();
            JPanel row = UiTheme.panel(new BorderLayout(6, 0));
            row.add(new JLabel(letter + ")"), BorderLayout.WEST);
            row.add(tf, BorderLayout.CENTER);
            row.add(rb, BorderLayout.EAST);
            txtOptions.add(tf);
            rbCorrect.add(rb);
            p.add(row);
        }
        return p;
    }

    private JPanel classificationPanel() {
        JPanel p = UiTheme.panel(new GridLayout(4, 2, 6, 6));
        p.add(new JLabel("Competencia:"));
        p.add(txtCompetence);
        p.add(new JLabel("Tema:"));
        p.add(txtTopic);
        p.add(new JLabel("Subtema:"));
        p.add(txtSubtopic);
        p.add(new JLabel("Nivel de dificultad:"));
        p.add(cmbDifficulty);
        cmbDifficulty.setSelectedItem("MEDIO");
        return p;
    }

    private void fill(QuestionDto q) {
        txtContext.setText(q.context());
        txtStatement.setText(q.statement());
        for (int i = 0; i < LETTERS.length && i < q.options().size(); i++) {
            txtOptions.get(i).setText(q.options().get(i));
            rbCorrect.get(i).setSelected(q.options().get(i).equals(q.correctAnswer()));
        }
        txtJustification.setText(q.justification());
        txtBibliography.setText(q.bibliography());
        txtCompetence.setText(q.competence());
        txtTopic.setText(q.topic());
        txtSubtopic.setText(q.subtopic());
        cmbDifficulty.setSelectedItem(q.difficulty());
    }

    private void save() {
        List<String> options = txtOptions.stream().map(t -> t.getText().trim()).toList();
        String correct = null;
        for (int i = 0; i < rbCorrect.size(); i++) {
            if (rbCorrect.get(i).isSelected()) {
                correct = options.get(i);
            }
        }
        QuestionRequestDto request = new QuestionRequestDto("MULTIPLE_CHOICE", txtContext.getText().trim(),
                txtStatement.getText().trim(), options, correct, txtJustification.getText().trim(),
                txtBibliography.getText().trim(), txtCompetence.getText().trim(), txtTopic.getText().trim(),
                txtSubtopic.getText().trim(), (String) cmbDifficulty.getSelectedItem());
        app.author().save(this, editing == null ? null : editing.id(), request, q -> {
            JOptionPane.showMessageDialog(this, "Pregunta guardada en estado \"" + q.stateLabel() + "\".",
                    "Banco de preguntas", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        });
    }
}
