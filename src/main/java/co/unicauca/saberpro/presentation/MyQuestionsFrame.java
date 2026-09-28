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
import java.util.ArrayList;
import java.util.List;

/**
 * Lista las preguntas creadas por el autor autenticado, con paginacion y
 * filtros (estado, tema, palabra clave), mostrando el estado de cada una
 * con su color correspondiente (HU02) y una leyenda de colores.
 *
 * HU03 ("listar las preguntas que he creado para poder mas adelante verlas
 * y editarlas"): desde aqui se puede ver el detalle completo de una pregunta
 * y editar las que siguen en estado Borrador.
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
    private int totalPages = 1;
    private static final int PAGE_SIZE = 5;
    /** Preguntas de la pagina actual, en el mismo orden que las filas de la tabla. */
    private final List<Question> currentItems = new ArrayList<>();

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

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        UiTheme.applyBackground(top);
        top.add(filters);
        top.add(buildColorLegend());
        center.add(top, BorderLayout.NORTH);

        // --- tabla con color por estado ---
        table.getColumnModel().getColumn(4).setCellRenderer(new StateColorRenderer());
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Doble clic sobre una fila = ver detalle
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onViewDetail();
                }
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);

        // --- pie: paginacion + accion ---
        JPanel south = new JPanel(new BorderLayout());
        UiTheme.applyBackground(south);
        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(pagination);
        JButton btnPrev = UiTheme.secondaryButton("<< Anterior");
        btnPrev.addActionListener(e -> { if (currentPage > 1) { currentPage--; loadData(); } });
        JButton btnNext = UiTheme.secondaryButton("Siguiente >>");
        btnNext.addActionListener(e -> { if (currentPage < totalPages) { currentPage++; loadData(); } });
        pagination.add(btnPrev);
        pagination.add(lblPageInfo);
        pagination.add(btnNext);
        south.add(pagination, BorderLayout.NORTH);

        JButton btnDetail = UiTheme.secondaryButton("Ver detalle");
        btnDetail.addActionListener(e -> onViewDetail());
        JButton btnEdit = UiTheme.secondaryButton("Editar");
        btnEdit.addActionListener(e -> onEdit());
        JButton btnChangeState = UiTheme.primaryButton("Enviar a revision");
        btnChangeState.addActionListener(e -> onChangeState());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(actions);
        actions.add(btnDetail);
        actions.add(btnEdit);
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

        totalPages = result.getTotalPages();
        if (currentPage > totalPages) {
            // Por ejemplo, tras aplicar un filtro o si la ultima pagina quedo vacia.
            currentPage = totalPages;
            loadData();
            return;
        }
        currentPage = result.getPage();

        tableModel.setRowCount(0);
        currentItems.clear();
        currentItems.addAll(result.getItems());
        for (Question q : result.getItems()) {
            tableModel.addRow(new Object[]{q.getId().substring(0, 8), q.getTopic(), q.getCompetence(),
                    q.getDirectQuestion(), q.getState().getLabel()});
        }

        lblPageInfo.setText(String.format(" Pagina %d de %d (%d preguntas) ",
                result.getPage(), result.getTotalPages(), result.getTotalItems()));
    }

    /** @return la pregunta seleccionada en la tabla, o null (avisando al usuario). */
    private Question selectedQuestion() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= currentItems.size()) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la tabla.",
                    "Sin seleccion", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return currentItems.get(row);
    }

    /** HU03: ver todos los datos de una pregunta. */
    private void onViewDetail() {
        Question q = selectedQuestion();
        if (q == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(q.getId()).append('\n');
        sb.append("Estado: ").append(q.getState().getLabel()).append('\n');
        sb.append("Creada: ").append(q.getCreatedAt()).append("\n\n");
        sb.append("CONTEXTO\n").append(q.getContext()).append("\n\n");
        sb.append("PREGUNTA DIRECTA\n").append(q.getDirectQuestion()).append("\n\n");
        sb.append("OPCIONES\n");
        List<String> options = q.getDistractors();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            sb.append("  ").append((char) ('A' + i)).append(") ").append(option);
            if (option.equals(q.getCorrectAnswer())) {
                sb.append("   <-- respuesta correcta");
            }
            sb.append('\n');
        }
        sb.append("\nJUSTIFICACION\n").append(q.getJustification()).append("\n\n");
        sb.append("BIBLIOGRAFIA\n").append(q.getBibliography()).append("\n\n");
        sb.append("Competencia: ").append(q.getCompetence()).append('\n');
        sb.append("Tema: ").append(q.getTopic()).append('\n');
        sb.append("Subtema: ").append(q.getSubtopic()).append('\n');
        sb.append("Nivel de dificultad: ").append(q.getDifficultyLevel()).append('\n');
        if (!q.getAssignedReviewers().isEmpty()) {
            sb.append("Revisores asignados: ").append(String.join(", ", q.getAssignedReviewers())).append('\n');
        }

        JTextArea area = new JTextArea(sb.toString(), 22, 60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Detalle de la pregunta", JOptionPane.PLAIN_MESSAGE);
    }

    /** HU03: editar una pregunta propia que sigue en estado Borrador. */
    private void onEdit() {
        Question q = selectedQuestion();
        if (q == null) {
            return;
        }
        if (q.getState() != QuestionState.BORRADOR) {
            JOptionPane.showMessageDialog(this,
                    "Solo se pueden editar preguntas en estado Borrador.\n"
                            + "Esta pregunta esta en estado: " + q.getState().getLabel(),
                    "No editable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new CreateQuestionFrame(context, q).setVisible(true);
        // La tabla se refresca sola al guardar: QuestionService notifica a los Observer.
    }

    private void onChangeState() {
        Question full = selectedQuestion();
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

    /** HU02: leyenda con el color de cada estado. */
    private JPanel buildColorLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        UiTheme.applyBackground(legend);
        JLabel title = new JLabel("Estados:");
        title.setForeground(UiTheme.TEXT);
        legend.add(title);
        for (QuestionState state : QuestionState.values()) {
            JLabel chip = new JLabel(" " + state.getLabel() + " ");
            chip.setOpaque(true);
            chip.setBackground(state.getColor());
            chip.setForeground(Color.BLACK);
            chip.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            legend.add(chip);
        }
        return legend;
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
