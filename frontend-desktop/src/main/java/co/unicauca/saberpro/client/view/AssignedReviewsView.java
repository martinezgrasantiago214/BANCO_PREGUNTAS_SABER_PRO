package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.PageDto;
import co.unicauca.saberpro.client.api.dto.ReviewDto;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppObserver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/** HU05: el revisor lista las preguntas que le asignó el administrador. */
public class AssignedReviewsView extends JFrame implements AppObserver {

    private static final int PAGE_SIZE = 8;
    private static final String[][] FILTERS = {{"ASIGNADA", "Pendientes por evaluar"},
            {"COMPLETADA", "Ya evaluadas"}, {"", "Todas"}};
    private final DesktopApp app;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Pregunta directa", "Competencia", "Tema", "Asignada", "Mi evaluación", "Estado revisión"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<String> cmbFilter = new JComboBox<>();
    private final Pager pager = new Pager(p -> load());
    private final List<ReviewDto> rows = new ArrayList<>();

    public AssignedReviewsView(DesktopApp app) {
        super("Preguntas asignadas");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Preguntas asignadas para revisión", app.session().user().login()), BorderLayout.NORTH);

        JPanel center = UiTheme.panel(new BorderLayout(6, 6));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel filters = UiTheme.panel(new FlowLayout(FlowLayout.LEFT));
        for (String[] f : FILTERS) {
            cmbFilter.addItem(f[1]);
        }
        cmbFilter.addActionListener(e -> { pager.reset(); load(); });
        filters.add(new JLabel("Mostrar:"));
        filters.add(cmbFilter);
        center.add(filters, BorderLayout.NORTH);

        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(360);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    open();
                }
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(pager, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel actions = UiTheme.panel(new FlowLayout(FlowLayout.RIGHT));
        JButton evaluate = UiTheme.primary("Leer y evaluar");
        evaluate.addActionListener(e -> open());
        actions.add(evaluate);
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(980, 520);
        setLocationRelativeTo(null);
        app.events().subscribe(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                app.events().unsubscribe(AssignedReviewsView.this);
            }
        });
        load();
    }

    private void load() {
        String status = FILTERS[cmbFilter.getSelectedIndex()][0];
        app.reviews().assignedToMe(this, status.isEmpty() ? null : status, pager.page(), PAGE_SIZE, this::render);
    }

    private void render(PageDto<ReviewDto> page) {
        String me = app.session().user().login();
        rows.clear();
        rows.addAll(page.items());
        model.setRowCount(0);
        for (ReviewDto r : page.items()) {
            ReviewDto.Assignment mine = r.assignmentOf(me);
            String myStatus = mine == null ? "" : mine.verdict() == null ? "Pendiente" : mine.verdict();
            model.addRow(new Object[]{r.statement(), r.competence(), r.topic(),
                    UiTheme.date(mine == null ? r.assignedAt() : mine.assignedAt()), myStatus, r.statusLabel()});
        }
        pager.update(page.page(), page.totalPages(), page.totalItems());
    }

    private void open() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta.");
            return;
        }
        new EvaluateQuestionView(app, rows.get(row).questionId()).setVisible(true);
    }

    @Override
    public void onChange(AppChange change) {
        if (change == AppChange.REVIEWS || change == AppChange.NOTIFICATIONS) {
            load();
        }
    }
}
