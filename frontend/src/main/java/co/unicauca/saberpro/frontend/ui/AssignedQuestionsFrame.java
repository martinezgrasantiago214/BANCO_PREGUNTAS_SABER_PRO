package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.facade.BancoPreguntasFacade;
import co.unicauca.saberpro.frontend.model.Models.AssignmentView;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import co.unicauca.saberpro.frontend.observer.Observer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * HU05 (PSP-45): preguntas que el administrador le asigno al revisor
 * autenticado. La fachada pone el login de la sesion, asi que el revisor solo
 * ve las suyas. El estado de cada pregunta se pinta con su color (HU02).
 *
 * Patron Observer: la pantalla se suscribe a los cambios de preguntas de la
 * fachada y se recarga sola, por ejemplo despues de evaluar.
 */
public class AssignedQuestionsFrame extends JFrame implements Observer {

    private final AppContext context;
    private final AssignmentTableModel model = new AssignmentTableModel();
    private final JTable table = new JTable(model);
    private final JLabel lblStatus = new JLabel(" ");

    public AssignedQuestionsFrame(AppContext context) {
        super("Preguntas asignadas");
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
        setSize(820, 480);
        setMinimumSize(new Dimension(600, 360));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Preguntas asignadas para revisar"), BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(AssignmentTableModel.COL_QUESTION).setPreferredWidth(420);
        table.getColumnModel().getColumn(AssignmentTableModel.COL_STATUS).setPreferredWidth(150);
        table.getColumnModel().getColumn(AssignmentTableModel.COL_STATUS).setCellRenderer(new StatusCellRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    openDetail();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setBorder(new EmptyBorder(12, 16, 4, 16));
        UiTheme.applyBackground(center);
        center.add(scroll, BorderLayout.CENTER);
        lblStatus.setForeground(UiTheme.TEXT);
        center.add(lblStatus, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        root.add(buildButtonBar(), BorderLayout.SOUTH);
        setContentPane(root);
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

        JButton btnDetail = UiTheme.secondaryButton("Ver detalle");
        btnDetail.addActionListener(e -> openDetail());
        bar.add(btnDetail);

        JButton btnEvaluate = UiTheme.primaryButton("Evaluar");
        btnEvaluate.addActionListener(e -> {
            Row row = selectedRow();
            if (row != null) {
                evaluate(row);
            }
        });
        bar.add(btnEvaluate);
        return bar;
    }

    /** Fila seleccionada (teniendo en cuenta el ordenamiento de la tabla) o null si no hay ninguna. */
    private Row selectedRow() {
        int viewIndex = table.getSelectedRow();
        if (viewIndex < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la lista.", "Preguntas asignadas",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return model.rowAt(table.convertRowIndexToModel(viewIndex));
    }

    /** HU05 (PSP-46): abre la pregunta seleccionada en modo solo lectura. */
    private void openDetail() {
        Row row = selectedRow();
        if (row == null || !isAvailable(row)) {
            return;
        }
        QuestionDetailDialog detail = new QuestionDetailDialog(this, context, row.question());
        if (row.isPending()) {
            JButton btnEvaluate = UiTheme.primaryButton("Evaluar");
            btnEvaluate.addActionListener(e -> {
                detail.dispose();
                evaluate(row);
            });
            detail.addAction(btnEvaluate);
        }
        detail.setVisible(true);
    }

    /** HU05 (PSP-47): abre el formulario de evaluacion si la pregunta sigue Pendiente de revision. */
    private void evaluate(Row row) {
        if (!isAvailable(row)) {
            return;
        }
        if (!row.isPending()) {
            JOptionPane.showMessageDialog(this, "La pregunta esta en estado " + row.question().statusLabel()
                            + ". Solo se pueden evaluar preguntas Pendientes de revision.",
                    "Evaluar pregunta", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EvaluationDialog dialog = new EvaluationDialog(this, context, row.question());
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            // El question-service cambia el estado al recibir el evento, un instante despues:
            // se vuelve a cargar la lista para mostrar el estado nuevo.
            Timer timer = new Timer(1500, e -> reload());
            timer.setRepeats(false);
            timer.start();
        }
    }

    private boolean isAvailable(Row row) {
        if (row.question() == null) {
            JOptionPane.showMessageDialog(this, "No fue posible obtener la pregunta " + row.assignment().questionId()
                    + " del question-service. Intente actualizar la lista.", "Pregunta no disponible",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Carga las asignaciones del revisor y, por cada una, la pregunta (texto y
     * estado) desde el question-service, en segundo plano para no congelar la
     * ventana.
     */
    private void reload() {
        lblStatus.setText("Cargando...");
        new SwingWorker<List<Row>, Void>() {
            @Override
            protected List<Row> doInBackground() {
                BancoPreguntasFacade facade = context.getFacade();
                List<Row> rows = new ArrayList<>();
                for (AssignmentView assignment : facade.listMyAssignments()) {
                    QuestionView question = null;
                    try {
                        question = facade.findQuestion(assignment.questionId());
                    } catch (ApiException ex) {
                        // La fila se muestra igual, marcada como no disponible.
                    }
                    rows.add(new Row(assignment, question));
                }
                return rows;
            }

            @Override
            protected void done() {
                try {
                    List<Row> rows = get();
                    model.setRows(rows);
                    lblStatus.setText(rows.isEmpty()
                            ? "No tiene preguntas asignadas."
                            : rows.size() + " pregunta(s) asignada(s).");
                } catch (ExecutionException ex) {
                    model.setRows(List.of());
                    lblStatus.setText("No fue posible cargar las preguntas asignadas.");
                    if (ex.getCause() instanceof ApiException api) {
                        Dialogs.showApiError(AssignedQuestionsFrame.this, api, "Preguntas asignadas");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Una asignacion con los datos de su pregunta (null si el question-service no la devolvio). */
    record Row(AssignmentView assignment, QuestionView question) {

        boolean isPending() {
            return question != null && question.status() == QuestionStatus.PENDIENTE_REVISION;
        }

        String questionText() {
            return question != null ? question.directQuestion()
                    : "(pregunta " + assignment.questionId() + " no disponible)";
        }
    }

    private static final class AssignmentTableModel extends AbstractTableModel {

        static final int COL_QUESTION = 0;
        static final int COL_STATUS = 1;
        private static final String[] COLUMNS = {"Pregunta", "Estado", "Asignada por", "Fecha de asignacion"};

        private List<Row> rows = List.of();

        void setRows(List<Row> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        Row rowAt(int index) {
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
            Row row = rows.get(rowIndex);
            return switch (columnIndex) {
                case COL_QUESTION -> row.questionText();
                case COL_STATUS -> row;
                case 2 -> row.assignment().assignedBy();
                default -> Formats.dateTime(row.assignment().assignedAt());
            };
        }
    }

    /** Pinta la celda del estado con el color que devuelve el API (statusColor). */
    private static final class StatusCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Row data = (Row) value;
            QuestionView question = data.question();
            super.getTableCellRendererComponent(table, question != null ? question.statusLabel() : "-",
                    isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                setBackground(question != null ? Formats.color(question.statusColor()) : table.getBackground());
                setForeground(UiTheme.TEXT);
            }
            return this;
        }
    }
}
