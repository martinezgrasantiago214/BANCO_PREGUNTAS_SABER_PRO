package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * HU05 (PSP-47): el revisor registra su veredicto (Aprobada o Rechazada) y
 * las observaciones que leera el autor. Al rechazar, las observaciones son
 * obligatorias. El review-service vuelve a validar todo al guardar.
 */
public class EvaluationDialog extends JDialog {

    /** Igual que EvaluationPolicy.MAX_OBSERVATIONS_LENGTH en el review-service. */
    static final int MAX_OBSERVATIONS_LENGTH = 2000;

    private final AppContext context;
    private final QuestionView question;
    private final JRadioButton rbApprove = new JRadioButton("Aprobar");
    private final JRadioButton rbReject = new JRadioButton("Rechazar");
    private final JTextArea txtObservations = new JTextArea(8, 40);
    private final JLabel lblCounter = new JLabel();
    private final JLabel lblMessage = new JLabel(" ");
    private boolean saved;

    public EvaluationDialog(Window owner, AppContext context, QuestionView question) {
        super(owner, "Evaluar pregunta", ModalityType.APPLICATION_MODAL);
        this.context = context;
        this.question = question;
        buildUi();
    }

    /** @return true si la evaluacion quedo registrada. */
    public boolean isSaved() {
        return saved;
    }

    private void buildUi() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(600, 520);
        setMinimumSize(new Dimension(480, 420));
        setLocationRelativeTo(getOwner());

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Evaluar pregunta"), BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(12, 16, 12, 16));
        UiTheme.applyBackground(form);

        form.add(QuestionDetailDialog.labeled("Pregunta",
                QuestionDetailDialog.readOnly(question.directQuestion())));
        form.add(Box.createVerticalStrut(8));

        ButtonGroup group = new ButtonGroup();
        group.add(rbApprove);
        group.add(rbReject);
        JPanel verdict = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        verdict.setOpaque(false);
        for (JRadioButton rb : new JRadioButton[]{rbApprove, rbReject}) {
            rb.setOpaque(false);
            rb.setForeground(UiTheme.TEXT);
            rb.addActionListener(e -> updateHints());
            verdict.add(rb);
        }
        form.add(QuestionDetailDialog.labeled("Veredicto *", verdict));
        form.add(Box.createVerticalStrut(8));

        txtObservations.setLineWrap(true);
        txtObservations.setWrapStyleWord(true);
        txtObservations.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateHints();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateHints();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateHints();
            }
        });
        JPanel observations = QuestionDetailDialog.labeled("Observaciones para el autor",
                new JScrollPane(txtObservations));
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        lblMessage.setFont(lblMessage.getFont().deriveFont(11f));
        footer.add(lblMessage, BorderLayout.CENTER);
        lblCounter.setFont(lblCounter.getFont().deriveFont(11f));
        footer.add(lblCounter, BorderLayout.EAST);
        observations.add(footer, BorderLayout.SOUTH);
        form.add(observations);

        root.add(form, BorderLayout.CENTER);
        root.add(buildButtonBar(), BorderLayout.SOUTH);
        setContentPane(root);
        updateHints();
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        UiTheme.applyBackground(bar);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.PRIMARY));

        JButton btnCancel = UiTheme.secondaryButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        bar.add(btnCancel);

        JButton btnSave = UiTheme.primaryButton("Registrar evaluacion");
        btnSave.addActionListener(e -> onSave());
        bar.add(btnSave);
        return bar;
    }

    private EvaluationVerdict selectedVerdict() {
        if (rbApprove.isSelected()) {
            return EvaluationVerdict.APROBADA;
        }
        return rbReject.isSelected() ? EvaluationVerdict.RECHAZADA : null;
    }

    /** Ayuda bajo las observaciones y contador "n/max" (en rojo si se pasa del tope). */
    private void updateHints() {
        int length = txtObservations.getText().trim().length();
        lblCounter.setText(length + "/" + MAX_OBSERVATIONS_LENGTH);
        lblCounter.setForeground(length > MAX_OBSERVATIONS_LENGTH ? UiTheme.ERROR : Color.GRAY);
        lblMessage.setForeground(Color.GRAY);
        lblMessage.setText(selectedVerdict() == EvaluationVerdict.RECHAZADA
                ? "Obligatorias al rechazar: indique que debe corregir el autor."
                : "Opcionales al aprobar.");
    }

    /** @return el mensaje de error o null si el formulario es valido. */
    private String validateForm() {
        EvaluationVerdict verdict = selectedVerdict();
        String text = txtObservations.getText().trim();
        if (verdict == null) {
            return "Seleccione si aprueba o rechaza la pregunta.";
        }
        if (verdict == EvaluationVerdict.RECHAZADA && text.isEmpty()) {
            return "Al rechazar una pregunta debe dejar observaciones para el autor.";
        }
        if (text.length() > MAX_OBSERVATIONS_LENGTH) {
            return "Las observaciones no pueden superar " + MAX_OBSERVATIONS_LENGTH + " caracteres.";
        }
        return null;
    }

    private void onSave() {
        String error = validateForm();
        if (error != null) {
            lblMessage.setForeground(UiTheme.ERROR);
            lblMessage.setText(error);
            return;
        }
        EvaluationVerdict verdict = selectedVerdict();
        int answer = JOptionPane.showConfirmDialog(this,
                "La pregunta quedara " + verdict.getLabel() + " y se notificara al autor y al administrador.\n"
                        + "Esta accion no se puede deshacer. ¿Desea continuar?",
                "Confirmar evaluacion", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            context.getFacade().evaluate(question.id(), verdict, txtObservations.getText().trim());
        } catch (ApiException ex) {
            Dialogs.showApiError(this, ex, "No fue posible registrar la evaluacion");
            return;
        }
        saved = true;
        JOptionPane.showMessageDialog(this, "Evaluacion registrada: la pregunta quedo " + verdict.getLabel() + ".",
                "Exito", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}
