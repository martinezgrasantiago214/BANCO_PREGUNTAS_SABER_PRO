package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.facade.BancoPreguntasFacade;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.PendingQuestionView;
import co.unicauca.saberpro.frontend.model.Models.UserView;
import co.unicauca.saberpro.frontend.observer.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * HU04: pantalla del administrador para asignar revisores.
 *
 * - PSP-30: lista las preguntas Pendientes de revision con los revisores que
 *   ya tienen asignados.
 * - PSP-31: muestra los usuarios con rol Revisor (seleccion multiple), sin el
 *   autor de la pregunta ni los que ya estan asignados, y controla que la
 *   pregunta quede con entre 1 y 3 revisores.
 * - PSP-32: asigna los revisores; el backend valida de nuevo y avisa por correo.
 *
 * Patron Observer: la pantalla se suscribe a los cambios de preguntas de la
 * fachada y se recarga sola despues de asignar.
 */
public class AssignReviewersFrame extends JFrame implements Observer {

    /** Maximo de revisores por pregunta (ReviewPolicy.MAX_REVIEWERS en el review-service). */
    static final int MAX_REVIEWERS = 3;

    private final AppContext context;
    private final PendingTableModel model = new PendingTableModel();
    private final JTable table = new JTable(model);
    private final DefaultListModel<UserView> reviewerModel = new DefaultListModel<>();
    private final JList<UserView> reviewerList = new JList<>(reviewerModel);
    private final JLabel lblReviewersHint = new JLabel(" ");
    private final JLabel lblStatus = new JLabel(" ");
    private List<UserView> allReviewers = List.of();

    public AssignReviewersFrame(AppContext context) {
        super("Asignar revisores");
        this.context = context;
        buildUi();
        context.getFacade().questionChanges().agregarObserver(this);
        reload();
    }

    @Override
    public void actualizar() {
        SwingUtilities.invokeLater(this::reload);
    }

    @Override
    public void dispose() {
        context.getFacade().questionChanges().eliminarObserver(this);
        super.dispose();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 520);
        setMinimumSize(new Dimension(760, 400));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Asignar revisores a preguntas pendientes"), BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(PendingTableModel.COL_QUESTION).setPreferredWidth(380);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshReviewerList();
            }
        });

        JPanel left = new JPanel(new BorderLayout(0, 6));
        UiTheme.applyBackground(left);
        left.add(label("Preguntas Pendientes de revision"), BorderLayout.NORTH);
        left.add(new JScrollPane(table), BorderLayout.CENTER);

        reviewerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        reviewerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                UserView user = (UserView) value;
                return super.getListCellRendererComponent(list, user.fullName() + " (" + user.login() + ")",
                        index, isSelected, cellHasFocus);
            }
        });

        JPanel right = new JPanel(new BorderLayout(0, 6));
        UiTheme.applyBackground(right);
        right.setPreferredSize(new Dimension(280, 0));
        right.add(label("Revisores (Ctrl+clic para elegir varios)"), BorderLayout.NORTH);
        right.add(new JScrollPane(reviewerList), BorderLayout.CENTER);
        lblReviewersHint.setForeground(UiTheme.TEXT);
        right.add(lblReviewersHint, BorderLayout.SOUTH);

        JPanel center = new JPanel(new BorderLayout(12, 6));
        center.setBorder(new EmptyBorder(12, 16, 4, 16));
        UiTheme.applyBackground(center);
        center.add(left, BorderLayout.CENTER);
        center.add(right, BorderLayout.EAST);
        lblStatus.setForeground(UiTheme.TEXT);
        center.add(lblStatus, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        root.add(buildButtonBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        UiTheme.applyBackground(bar);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.PRIMARY));

        JButton btnClose = UiTheme.secondaryButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        bar.add(btnClose);

        JButton btnReload = UiTheme.secondaryButton("Actualizar");
        btnReload.addActionListener(e -> reload());
        bar.add(btnReload);

        JButton btnAssign = UiTheme.primaryButton("Asignar revisores");
        btnAssign.addActionListener(e -> assign());
        bar.add(btnAssign);
        return bar;
    }

    private PendingQuestionView selectedQuestion() {
        int viewIndex = table.getSelectedRow();
        return viewIndex < 0 ? null : model.rowAt(table.convertRowIndexToModel(viewIndex));
    }

    /** PSP-31: solo revisores que no son el autor ni estan asignados ya a la pregunta seleccionada. */
    private void refreshReviewerList() {
        reviewerModel.clear();
        PendingQuestionView question = selectedQuestion();
        if (question == null) {
            lblReviewersHint.setText("Seleccione una pregunta.");
            return;
        }
        for (UserView reviewer : selectableReviewers(allReviewers, question)) {
            reviewerModel.addElement(reviewer);
        }
        int remaining = remainingSlots(question);
        lblReviewersHint.setText(remaining == 0
                ? "La pregunta ya tiene " + MAX_REVIEWERS + " revisores."
                : "Puede elegir hasta " + remaining + " revisor(es) mas.");
    }

    /** Revisores que se le pueden asignar a la pregunta: ni el autor ni los ya asignados. */
    static List<UserView> selectableReviewers(List<UserView> reviewers, PendingQuestionView question) {
        return reviewers.stream()
                .filter(r -> !r.login().equals(question.authorLogin()))
                .filter(r -> !question.assignedReviewers().contains(r.login()))
                .toList();
    }

    static int remainingSlots(PendingQuestionView question) {
        return Math.max(0, MAX_REVIEWERS - question.assignedReviewers().size());
    }

    /** PSP-32: asigna los revisores elegidos a la pregunta seleccionada. */
    private void assign() {
        PendingQuestionView question = selectedQuestion();
        if (question == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la lista.", "Asignar revisores",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<String> logins = reviewerList.getSelectedValuesList().stream().map(UserView::login).toList();
        if (logins.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleccione al menos un revisor.", "Asignar revisores",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (logins.size() > remainingSlots(question)) {
            JOptionPane.showMessageDialog(this, "Una pregunta puede tener maximo " + MAX_REVIEWERS
                    + " revisores; ya tiene " + question.assignedReviewers().size() + ".", "Asignar revisores",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<AssignmentView> created = context.getFacade().assignReviewers(question.questionId(), logins);
            JOptionPane.showMessageDialog(this, "Se asignaron " + created.size() + " revisor(es): "
                    + String.join(", ", logins) + ".\nCada revisor recibira un correo de notificacion.",
                    "Asignar revisores", JOptionPane.INFORMATION_MESSAGE);
        } catch (ApiException ex) {
            Dialogs.showApiError(this, ex, "Asignar revisores");
        }
    }

    /** Carga las preguntas pendientes y los revisores en segundo plano para no congelar la ventana. */
    private void reload() {
        lblStatus.setText("Cargando...");
        String selectedId = selectedQuestion() != null ? selectedQuestion().questionId() : null;
        new SwingWorker<Data, Void>() {
            @Override
            protected Data doInBackground() {
                BancoPreguntasFacade facade = context.getFacade();
                return new Data(facade.listPendingQuestions(), facade.listReviewers());
            }

            @Override
            protected void done() {
                try {
                    Data data = get();
                    allReviewers = data.reviewers();
                    model.setRows(data.pending());
                    lblStatus.setText(data.pending().isEmpty()
                            ? "No hay preguntas Pendientes de revision."
                            : data.pending().size() + " pregunta(s) pendiente(s) de revision.");
                    reselect(selectedId);
                } catch (ExecutionException ex) {
                    model.setRows(List.of());
                    lblStatus.setText("No fue posible cargar las preguntas pendientes.");
                    if (ex.getCause() instanceof ApiException api) {
                        Dialogs.showApiError(AssignReviewersFrame.this, api, "Asignar revisores");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                refreshReviewerList();
            }
        }.execute();
    }

    private void reselect(String questionId) {
        for (int i = 0; questionId != null && i < model.getRowCount(); i++) {
            if (model.rowAt(i).questionId().equals(questionId)) {
                int viewIndex = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
                return;
            }
        }
    }

    private record Data(List<PendingQuestionView> pending, List<UserView> reviewers) {
    }

    private static final class PendingTableModel extends AbstractTableModel {

        static final int COL_QUESTION = 0;
        private static final String[] COLUMNS = {"Pregunta", "Autor", "Enviada a revision", "Revisores asignados"};

        private List<PendingQuestionView> rows = List.of();

        void setRows(List<PendingQuestionView> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        PendingQuestionView rowAt(int index) {
            return rows.get(index);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PendingQuestionView row = rows.get(rowIndex);
            return switch (columnIndex) {
                case COL_QUESTION -> row.directQuestion() != null ? row.directQuestion() : row.questionId();
                case 1 -> row.authorLogin();
                case 2 -> Formats.dateTime(row.submittedAt());
                default -> row.assignedReviewers().isEmpty() ? "(ninguno)"
                        : String.join(", ", row.assignedReviewers());
            };
        }
    }
}
