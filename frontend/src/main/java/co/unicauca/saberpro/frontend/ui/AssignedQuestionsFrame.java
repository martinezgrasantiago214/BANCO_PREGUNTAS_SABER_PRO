package co.unicauca.saberpro.frontend.ui;

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
        return bar;
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
