package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.SaberProApiFacade.ReviewWorkspace;
import co.unicauca.saberpro.client.api.dto.CriterionDto;
import co.unicauca.saberpro.client.api.dto.EvaluationRequestDto;
import co.unicauca.saberpro.client.api.dto.ReviewDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * HU05: el revisor lee la pregunta completa, califica la rúbrica (1-5 por
 * criterio), emite su concepto Aprobada/Rechazada y deja observaciones para
 * el autor. Si ya evaluó, la pantalla queda en solo lectura.
 */
public class EvaluateQuestionView extends JFrame {

    private final DesktopApp app;
    private final String questionId;
    private final JPanel content = UiTheme.panel(new BorderLayout(8, 8));
    private final List<JSpinner> spinners = new ArrayList<>();
    private final List<JTextField> comments = new ArrayList<>();
    private final JRadioButton rbApprove = new JRadioButton("Aprobada");
    private final JRadioButton rbReject = new JRadioButton("Rechazada");
    private final JTextArea txtObservation = UiTheme.textArea(3);
    private List<CriterionDto> criteria = List.of();

    public EvaluateQuestionView(DesktopApp app, String questionId) {
        super("Evaluar pregunta");
        this.app = app;
        this.questionId = questionId;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Revisión por pares", "HU05"), BorderLayout.NORTH);
        content.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        reload();
    }

    private void reload() {
        app.reviews().workspace(this, questionId, this::render);
    }

    private void render(ReviewWorkspace ws) {
        content.removeAll();
        criteria = ws.criteria();
        JScrollPane questionScroll = new JScrollPane(QuestionDetailDialog.questionPanel(ws.question(), true));
        questionScroll.setBorder(UiTheme.titled("Pregunta"));

        ReviewDto.Assignment mine = ws.review().assignmentOf(app.session().user().login());
        boolean canEvaluate = mine != null && mine.verdict() == null && "EN_REVISION".equals(ws.review().status());
        JPanel right = UiTheme.panel(new BorderLayout(6, 6));
        right.add(canEvaluate ? evaluationForm() : summary(mine), BorderLayout.NORTH);
        JPanel obs = UiTheme.panel(new BorderLayout(4, 4));
        obs.setBorder(UiTheme.titled("Observaciones para el autor"));
        obs.add(new JScrollPane(QuestionDetailDialog.observationsPanel(ws.review().observations())), BorderLayout.CENTER);
        if ("EN_REVISION".equals(ws.review().status()) && mine != null) {
            JPanel add = UiTheme.panel(new BorderLayout(4, 4));
            JTextArea txt = UiTheme.textArea(2);
            JButton btn = UiTheme.secondary("Agregar observación");
            btn.addActionListener(e -> app.reviews().observe(this, questionId, txt.getText().trim(), o -> reload()));
            add.add(new JScrollPane(txt), BorderLayout.CENTER);
            add.add(btn, BorderLayout.EAST);
            obs.add(add, BorderLayout.SOUTH);
        }
        right.add(obs, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, questionScroll, right);
        split.setResizeWeight(0.55);
        content.add(split, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
    }

    private JPanel evaluationForm() {
        spinners.clear();
        comments.clear();
        JPanel form = UiTheme.panel(new BorderLayout(4, 4));
        form.setBorder(UiTheme.titled("Mi evaluación (rúbrica 1 a 5)"));
        JPanel grid = UiTheme.panel(new GridLayout(0, 3, 6, 4));
        for (CriterionDto c : criteria) {
            grid.add(new JLabel(c.label()));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(3, c.minScore(), c.maxScore(), 1));
            spinners.add(sp);
            grid.add(sp);
            JTextField comment = new JTextField();
            comment.setToolTipText("Comentario del criterio (opcional)");
            comments.add(comment);
            grid.add(comment);
        }
        form.add(grid, BorderLayout.NORTH);

        JPanel verdict = UiTheme.panel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup g = new ButtonGroup();
        g.add(rbApprove);
        g.add(rbReject);
        rbApprove.setBackground(UiTheme.BACKGROUND);
        rbReject.setBackground(UiTheme.BACKGROUND);
        verdict.add(new JLabel("Concepto:"));
        verdict.add(rbApprove);
        verdict.add(rbReject);
        JPanel middle = UiTheme.panel(new BorderLayout(4, 4));
        middle.add(verdict, BorderLayout.NORTH);
        JPanel obs = UiTheme.panel(new BorderLayout());
        obs.add(new JLabel("Observación para el autor (opcional):"), BorderLayout.NORTH);
        obs.add(new JScrollPane(txtObservation), BorderLayout.CENTER);
        middle.add(obs, BorderLayout.CENTER);
        form.add(middle, BorderLayout.CENTER);

        JButton submit = UiTheme.primary("Registrar evaluación");
        submit.addActionListener(e -> submit());
        JPanel btn = UiTheme.panel(new FlowLayout(FlowLayout.RIGHT));
        btn.add(submit);
        form.add(btn, BorderLayout.SOUTH);
        return form;
    }

    private JComponent summary(ReviewDto.Assignment mine) {
        if (mine == null || mine.verdict() == null) {
            return new JLabel("  La revisión ya no admite evaluaciones.");
        }
        StringBuilder sb = new StringBuilder("Su concepto: " + mine.verdict() + "  (promedio "
                + String.format("%.1f", mine.averageScore()) + "/5)\n");
        mine.scores().forEach(s -> sb.append(" • ").append(s.criterionLabel()).append(": ").append(s.score())
                .append(s.comment().isBlank() ? "" : " — " + s.comment()).append('\n'));
        JTextArea area = UiTheme.readOnly(sb.toString().trim());
        area.setBorder(UiTheme.titled("Evaluación registrada"));
        return area;
    }

    private void submit() {
        if (!rbApprove.isSelected() && !rbReject.isSelected()) {
            JOptionPane.showMessageDialog(this, "Indique si la pregunta queda Aprobada o Rechazada.");
            return;
        }
        List<EvaluationRequestDto.Score> scores = new ArrayList<>();
        for (int i = 0; i < criteria.size(); i++) {
            scores.add(new EvaluationRequestDto.Score(criteria.get(i).code(), (Integer) spinners.get(i).getValue(),
                    comments.get(i).getText().trim()));
        }
        String verdict = rbApprove.isSelected() ? "APROBADA" : "RECHAZADA";
        app.reviews().evaluate(this, questionId,
                new EvaluationRequestDto(verdict, scores, txtObservation.getText().trim()), review -> {
                    String msg = review.finalDecision() == null
                            ? "Evaluación registrada. La decisión final se tomará cuando evalúen los demás revisores."
                            : "Evaluación registrada. Decisión final de la pregunta: " + review.finalDecision() + ".";
                    JOptionPane.showMessageDialog(this, msg + "\nSe notificó a los implicados.");
                    reload();
                });
    }
}
