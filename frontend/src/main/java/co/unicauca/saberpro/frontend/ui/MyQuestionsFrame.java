package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.client.ApiException;
import co.unicauca.saberpro.frontend.model.Models.PageView;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;
import co.unicauca.saberpro.frontend.observer.Observer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla "Mis preguntas" (HU03, PSP-9): el autor ve las preguntas que ha
 * creado, las busca por palabra clave, las filtra por estado y tema, recorre
 * el listado por paginas y edita las que siguen en Borrador.
 *
 * <ul>
 *   <li>PSP-24 listado: tabla con las preguntas del autor autenticado, de la
 *       mas reciente a la mas antigua, y el estado con su color (HU02).</li>
 *   <li>PSP-25 busqueda: palabra clave en el enunciado, el contexto y el tema.</li>
 *   <li>PSP-26 filtros: estado y tema.</li>
 *   <li>PSP-27 paginacion: anterior/siguiente y tamano de pagina.</li>
 *   <li>PSP-28 edicion: abre {@link CreateQuestionFrame} en modo edicion.</li>
 * </ul>
 *
 * Toda la comunicacion pasa por la fachada (BancoPreguntasFacade), que llama
 * al question-service a traves del API Gateway. La pantalla es Observer de
 * los cambios en preguntas: al guardar una edicion se refresca sola.
 */
public class MyQuestionsFrame extends JFrame implements Observer {

    private static final String ALL_STATES = "Todos los estados";
    private static final String[] COLUMNS = {"Pregunta", "Tema", "Competencia", "Dificultad", "Estado", "Creada"};
    private static final int STATUS_COLUMN = 4;

    private final AppContext context;
    private final MyQuestionsQuery query = new MyQuestionsQuery();
    /** Preguntas de la pagina actual, en el mismo orden que las filas de la tabla. */
    private final List<QuestionView> rows = new ArrayList<>();

    private final DefaultTableModel tableModel = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JTextField txtKeyword = new JTextField(16);
    private final JTextField txtTopic = new JTextField(12);
    private final JComboBox<Object> cmbStatus = new JComboBox<>();
    private final JComboBox<Integer> cmbPageSize = new JComboBox<>();
    private final JLabel lblSummary = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel lblEmpty = new JLabel(" ", SwingConstants.CENTER);
    private final JButton btnPrevious = UiTheme.secondaryButton("< Anterior");
    private final JButton btnNext = UiTheme.secondaryButton("Siguiente >");
    private final JButton btnDetail = UiTheme.secondaryButton("Ver detalle");
    private final JButton btnEdit = UiTheme.primaryButton("Editar");
    /** Evita que limpiar los campos dispare una busqueda por cada campo que se borra. */
    private boolean resettingFilters;

    public MyQuestionsFrame(AppContext context) {
        super("Mis preguntas");
        this.context = context;
        buildUi();
        context.getFacade().questionChanges().agregarObserver(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                context.getFacade().questionChanges().eliminarObserver(MyQuestionsFrame.this);
            }
        });
        loadPage();
    }

    /** Observer: alguna pregunta cambio (p. ej. se guardo una edicion); se recarga la pagina actual. */
    @Override
    public void actualizar() {
        loadPage();
    }

    // ---------------------------------------------------------------- UI

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(980, 620);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Mis preguntas"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(new EmptyBorder(12, 14, 8, 14));
        UiTheme.applyBackground(center);

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        UiTheme.applyBackground(top);
        top.add(buildFilters());
        top.add(buildLegend());
        center.add(top, BorderLayout.NORTH);

        configureTable();
        JPanel tablePanel = new JPanel(new BorderLayout());
        UiTheme.applyBackground(tablePanel);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        lblEmpty.setForeground(UiTheme.TEXT);
        lblEmpty.setFont(lblEmpty.getFont().deriveFont(Font.ITALIC));
        tablePanel.add(lblEmpty, BorderLayout.SOUTH);
        center.add(tablePanel, BorderLayout.CENTER);

        center.add(buildFooter(), BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
    }

    /** PSP-25 y PSP-26: busqueda por palabra clave y filtros por estado y tema. */
    private JPanel buildFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        UiTheme.applyBackground(filters);

        cmbStatus.addItem(ALL_STATES);
        for (QuestionStatus status : QuestionStatus.values()) {
            if (status != QuestionStatus.ELIMINADA) {
                cmbStatus.addItem(status);
            }
        }
        txtKeyword.setToolTipText("Busca en el enunciado, el contexto y el tema de la pregunta.");
        txtTopic.setToolTipText("Muestra las preguntas cuyo tema contiene este texto.");

        filters.add(label("Buscar:"));
        filters.add(txtKeyword);
        filters.add(label("Estado:"));
        filters.add(cmbStatus);
        filters.add(label("Tema:"));
        filters.add(txtTopic);

        JButton btnSearch = UiTheme.primaryButton("Buscar");
        btnSearch.addActionListener(e -> applyFilters());
        JButton btnClear = UiTheme.secondaryButton("Limpiar");
        btnClear.addActionListener(e -> clearFilters());
        filters.add(btnSearch);
        filters.add(btnClear);

        // Enter en los campos de texto o cambiar el estado tambien buscan.
        txtKeyword.addActionListener(e -> applyFilters());
        txtTopic.addActionListener(e -> applyFilters());
        cmbStatus.addActionListener(e -> applyFilters());
        return filters;
    }

    /** Leyenda de colores de los estados (HU02). */
    private JPanel buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        UiTheme.applyBackground(legend);
        legend.add(label("Estados:"));
        for (QuestionStatus status : QuestionStatus.values()) {
            if (status == QuestionStatus.ELIMINADA) {
                continue;
            }
            JLabel chip = new JLabel(" " + status.getLabel() + " ");
            chip.setOpaque(true);
            chip.setBackground(Color.decode(status.getColorHex()));
            chip.setForeground(UiTheme.TEXT);
            chip.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            legend.add(chip);
        }
        return legend;
    }

    private void configureTable() {
        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(STATUS_COLUMN).setPreferredWidth(140);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(STATUS_COLUMN).setCellRenderer(new StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    showDetail();
                }
            }
        });
    }

    /** PSP-27: paginacion; y las acciones sobre la pregunta seleccionada (PSP-28). */
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        UiTheme.applyBackground(footer);

        JPanel pagination = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        UiTheme.applyBackground(pagination);
        for (int size : MyQuestionsQuery.PAGE_SIZES) {
            cmbPageSize.addItem(size);
        }
        cmbPageSize.setSelectedItem(MyQuestionsQuery.DEFAULT_PAGE_SIZE);
        cmbPageSize.addActionListener(e -> {
            query.changePageSize((Integer) cmbPageSize.getSelectedItem());
            loadPage();
        });
        btnPrevious.addActionListener(e -> {
            if (query.previous()) {
                loadPage();
            }
        });
        btnNext.addActionListener(e -> {
            if (query.next()) {
                loadPage();
            }
        });
        lblSummary.setForeground(UiTheme.TEXT);
        pagination.add(btnPrevious);
        pagination.add(lblSummary);
        pagination.add(btnNext);
        pagination.add(Box.createHorizontalStrut(16));
        pagination.add(label("Por pagina:"));
        pagination.add(cmbPageSize);
        footer.add(pagination, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        UiTheme.applyBackground(actions);
        btnDetail.addActionListener(e -> showDetail());
        btnEdit.addActionListener(e -> editSelected());
        JButton btnClose = UiTheme.secondaryButton("Cerrar");
        btnClose.addActionListener(e -> dispose());
        // TODO(PSP-22): boton "Enviar a revision" con facade.submitForReview (HU02).
        actions.add(btnDetail);
        actions.add(btnEdit);
        actions.add(btnClose);
        footer.add(actions, BorderLayout.SOUTH);
        return footer;
    }

    // ---------------------------------------------------------------- comportamiento

    private void applyFilters() {
        if (resettingFilters) {
            return;
        }
        Object selected = cmbStatus.getSelectedItem();
        QuestionStatus status = selected instanceof QuestionStatus s ? s : null;
        query.filter(status, txtTopic.getText(), txtKeyword.getText());
        loadPage();
    }

    private void clearFilters() {
        resettingFilters = true;
        try {
            txtKeyword.setText("");
            txtTopic.setText("");
            cmbStatus.setSelectedItem(ALL_STATES);
        } finally {
            resettingFilters = false;
        }
        query.clearFilters();
        loadPage();
    }

    /** Consulta al backend la pagina actual con los filtros vigentes y pinta la tabla. */
    private void loadPage() {
        PageView<QuestionView> result;
        try {
            result = context.getFacade().searchMyQuestions(query.status(), query.topic(), query.keyword(),
                    query.page(), query.pageSize());
        } catch (ApiException ex) {
            Dialogs.showApiError(this, ex, "No fue posible cargar sus preguntas");
            return;
        }
        query.update(result);

        String selectedId = selectedQuestion() != null ? selectedQuestion().id() : null;
        rows.clear();
        rows.addAll(result.items());
        tableModel.setRowCount(0);
        for (QuestionView q : rows) {
            tableModel.addRow(new Object[]{q.directQuestion(), q.topic(), q.competence(),
                    q.difficultyLevel() != null ? q.difficultyLevel().toString() : "", q,
                    shortDate(q.createdAt())});
        }
        reselect(selectedId);

        lblSummary.setText(query.summary());
        lblEmpty.setText(rows.isEmpty()
                ? (query.hasFilters() ? "Ninguna pregunta coincide con la busqueda o los filtros."
                                      : "Aun no ha creado preguntas. Use \"Crear pregunta\" en el menu principal.")
                : " ");
        btnPrevious.setEnabled(query.hasPrevious());
        btnNext.setEnabled(query.hasNext());
        updateActions();
    }

    private void reselect(String id) {
        for (int i = 0; id != null && i < rows.size(); i++) {
            if (rows.get(i).id().equals(id)) {
                table.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private QuestionView selectedQuestion() {
        int row = table.getSelectedRow();
        return row >= 0 && row < rows.size() ? rows.get(row) : null;
    }

    private void updateActions() {
        QuestionView q = selectedQuestion();
        btnDetail.setEnabled(q != null);
        btnEdit.setEnabled(q != null && q.editable());
        btnEdit.setToolTipText(q == null ? "Seleccione una pregunta."
                : q.editable() ? "Editar la pregunta seleccionada."
                : "Solo se pueden editar preguntas en Borrador (esta en " + q.statusLabel() + ").");
    }

    /** PSP-28: abre el formulario de HU01 en modo edicion con la version mas reciente de la pregunta. */
    private void editSelected() {
        QuestionView q = selectedQuestion();
        if (q == null || !q.editable()) {
            return;
        }
        QuestionView latest;
        try {
            latest = context.getFacade().findQuestion(q.id());
        } catch (ApiException ex) {
            Dialogs.showApiError(this, ex, "No fue posible abrir la pregunta");
            return;
        }
        if (!latest.editable()) {
            JOptionPane.showMessageDialog(this, "La pregunta ya no se puede editar: esta en "
                    + latest.statusLabel() + ".", "Edicion no disponible", JOptionPane.WARNING_MESSAGE);
            loadPage();
            return;
        }
        new CreateQuestionFrame(context, latest).setVisible(true);
    }

    /** Muestra todos los datos de la pregunta seleccionada (HU03: "verlas"). */
    private void showDetail() {
        QuestionView q = selectedQuestion();
        if (q == null) {
            return;
        }
        JTextArea area = new JTextArea(QuestionDetailFormatter.format(q), 24, 64);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Detalle de la pregunta",
                JOptionPane.PLAIN_MESSAGE);
    }

    private static String shortDate(String isoDateTime) {
        return isoDateTime != null && isoDateTime.length() >= 10 ? isoDateTime.substring(0, 10) : "";
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.TEXT);
        return label;
    }

    /** Pinta la celda de estado con el color que define el backend para ese estado (HU02). */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            QuestionView q = (QuestionView) value;
            Component cell = super.getTableCellRendererComponent(table, q.statusLabel(), isSelected, hasFocus,
                    row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(Color.decode(q.statusColor()));
            setForeground(UiTheme.TEXT);
            if (isSelected) {
                setBorder(BorderFactory.createLineBorder(UiTheme.PRIMARY, 2));
            }
            return cell;
        }
    }
}
