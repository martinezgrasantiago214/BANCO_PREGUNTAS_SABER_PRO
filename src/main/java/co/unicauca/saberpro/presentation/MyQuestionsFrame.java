package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.infra.Observer;
import co.unicauca.saberpro.service.OperationResult;
import co.unicauca.saberpro.service.PagedResult;
import co.unicauca.saberpro.service.QuestionFilterCriteria;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Lista las preguntas creadas por el autor autenticado, con paginacion y
 * filtros (estado, tema, palabra clave), mostrando el estado de cada una
 * con su color correspondiente.
 *
 * Implementa Observer (micro patron MVC + Observer) para refrescarse
 * automaticamente si el estado de alguna pregunta cambia desde otra parte
 * de la aplicacion (por ejemplo, cuando el administrador asigna revisores).
 */
public class MyQuestionsFrame extends JFrame implements Observer {

    private final AppContext context;
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"ID", "Tema", "Competencia", "Pregunta", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    private final JComboBox<String> cmbStateFilter = new JComboBox<>();
    private final JTextField txtTopicFilter = new JTextField(12);
    private final JTextField txtKeywordFilter = new JTextField(12);
    private final JLabel lblPageInfo = new JLabel();

    private int currentPage = 1;
    private static final int PAGE_SIZE = 5;

    public MyQuestionsFrame(AppContext context) {
        super("Mis preguntas");
        this.context = context;
        buildUi();
        context.getQuestionService().agregarObserver(this);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                context.getQuestionService().eliminarObserver(MyQuestionsFrame.this);
            }
        });
        loadData();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(860, 600);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Mis preguntas"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBorder(new EmptyBorder(12, 12, 12, 12));
        UiTheme.applyBackground(center);

        // --- filtros ---
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        UiTheme.applyBackground(filters);
        cmbStateFilter.addItem("(Todos los estados)");
        for (QuestionState state : QuestionState.values()) {
            cmbStateFilter.addItem(state.getLabel());
        }
        filters.add(new JLabel("Estado:"));
        filters.add(cmbStateFilter);
        filters.add(new JLabel("Tema:"));
        filters.add(txtTopicFilter);
        filters.add(new JLabel("Palabra clave:"));
        filters.add(txtKeywordFilter);
        JButton btnFilter = UiTheme.secondaryButton("Buscar");
        btnFilter.addActionListener(e -> { currentPage = 1; loadData(); });
        filters.add(btnFilter);
        center.add(filters, BorderLayout.NORTH);

        // --- tabla con color por estado ---
        table.getColumnModel().getColumn(4).setCellRenderer(new StateColorRenderer());
        table.setRowHeight(24);
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- pie: paginacion + accion ---
        JPanel south = new JPanel(new BorderLayout());
        UiTheme.applyBackground(south);
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(pagination);
        JButton btnPrev = UiTheme.secondaryButton("<< Anterior");
        btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; loadData(); } });
        JButton btnNext = UiTheme.secondaryButton("Siguiente >>");
        btnNext.addActionListener(e -> { currentPage++; loadData(); });
        pagination.add(btnPrev);
        pagination.add(lblPageInfo);
        pagination.add(btnNext);
        south.add(pagination, BorderLayout.NORTH);

        JButton btnChangeState = UiTheme.primaryButton("Enviar a revision");
        btnChangeState.addActionListener(e -> onChangeState());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(actions);
        actions.add(btnChangeState);
        south.add(actions, BorderLayout.SOUTH);

        center.add(south, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void loadData() {
        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin(context.getLoggedInUser().getLogin())
                .setPage(currentPage)
                .setPageSize(PAGE_SIZE);

        String stateSelected = (String) cmbStateFilter.getSelectedItem();
        if (stateSelected != null && !stateSelected.startsWith("(")) {
            for (QuestionState state : QuestionState.values()) {
                if (state.getLabel().equals(stateSelected)) {
                    criteria.setState(state);
                }
            }
        }
        if (!txtTopicFilter.getText().isBlank()) {
            criteria.setTopic(txtTopicFilter.getText().trim());
        }
        if (!txtKeywordFilter.getText().isBlank()) {
            criteria.setKeyword(txtKeywordFilter.getText().trim());
        }

        PagedResult<Question> result;
        try {
            result = context.getQuestionService().listarMisPreguntas(criteria);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ocurrio un error inesperado consultando las preguntas:\n" + ex,
                    "Error al listar preguntas", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return;
        }

        String dbError = context.getQuestionService().getUltimoErrorDeBaseDeDatos();
        if (dbError != null) {
            JOptionPane.showMessageDialog(this,
                    "No se pudieron consultar las preguntas en la base de datos.\n\n" + dbError,
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
        }

        currentPage = result.getPage();

        tableModel.setRowCount(0);
        for (Question q : result.getItems()) {
            tableModel.addRow(new Object[]{q.getId().substring(0, 8), q.getTopic(), q.getCompetence(),
                    q.getDirectQuestion(), q.getState().getLabel()});
        }

        lblPageInfo.setText(String.format(" Pagina %d de %d (%d preguntas) ",
                result.getPage(), result.getTotalPages(), result.getTotalItems()));
    }

    private void onChangeState() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la tabla.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String shortId = (String) tableModel.getValueAt(row, 0);
        Question full = findByShortId(shortId);
        if (full == null) {
            return;
        }

        OperationResult result = context.getQuestionService()
                .marcarPendienteDeRevision(full.getId(), context.getLoggedInUser().getLogin());

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Estado actualizado a 'Pendiente de revision'.");
            loadData();
        } else {
            JOptionPane.showMessageDialog(this, result.getErrorsAsText(),
                    "No fue posible cambiar el estado", JOptionPane.WARNING_MESSAGE);
        }
    }

    private Question findByShortId(String shortId) {
        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin(context.getLoggedInUser().getLogin())
                .setPage(1).setPageSize(1000);
        return context.getQuestionService().listarMisPreguntas(criteria).getItems().stream()
                .filter(q -> q.getId().startsWith(shortId))
                .findFirst().orElse(null);
    }

    @Override
    public void actualizar() {
        SwingUtilities.invokeLater(this::loadData);
    }

    /** Renderiza la celda de estado con el color definido en QuestionState. */
    private static class StateColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                         boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            for (QuestionState state : QuestionState.values()) {
                if (state.getLabel().equals(value)) {
                    c.setBackground(state.getColor());
                    c.setForeground(Color.BLACK);
                    break;
                }
            }
            if (isSelected) {
                c.setForeground(Color.BLUE);
            }
            return c;
        }
    }
}
