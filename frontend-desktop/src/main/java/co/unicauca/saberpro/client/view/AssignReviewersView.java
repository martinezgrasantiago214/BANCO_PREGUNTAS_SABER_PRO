package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.PageDto;
import co.unicauca.saberpro.client.api.dto.ReviewDto;
import co.unicauca.saberpro.client.api.dto.UserDto;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppObserver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/** HU04: el administrador asigna entre 1 y 3 revisores a las preguntas Pendientes de revisión. */
public class AssignReviewersView extends JFrame implements AppObserver {

    private static final int PAGE_SIZE = 8;
    private final DesktopApp app;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Pregunta directa", "Autor", "Competencia", "Tema", "Enviada", "Ronda"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final DefaultListModel<UserDto> reviewersModel = new DefaultListModel<>();
    private final JList<UserDto> lstReviewers = new JList<>(reviewersModel);
    private final JLabel lblSelection = new JLabel("Seleccionados: 0 / 3");
    private final Pager pager = new Pager(p -> load());
    private final List<ReviewDto> rows = new ArrayList<>();

    public AssignReviewersView(DesktopApp app) {
        super("Asignar revisores");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Preguntas pendientes de asignar revisores", "HU04"), BorderLayout.NORTH);

        JPanel left = UiTheme.panel(new BorderLayout(4, 4));
        left.setBorder(new EmptyBorder(10, 10, 10, 6));
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(320);
        left.add(new JScrollPane(table), BorderLayout.CENTER);
        left.add(pager, BorderLayout.SOUTH);

        JPanel right = UiTheme.panel(new BorderLayout(4, 4));
        right.setBorder(new EmptyBorder(10, 6, 10, 10));
        right.setPreferredSize(new Dimension(280, 0));
        JLabel help = new JLabel("<html>Revisores disponibles<br><small>Ctrl/Cmd + clic para seleccionar varios "
                + "(mínimo 1, máximo 3)</small></html>");
        right.add(help, BorderLayout.NORTH);
        lstReviewers.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lstReviewers.addListSelectionListener(e -> {
            int n = lstReviewers.getSelectedValuesList().size();
            lblSelection.setText("Seleccionados: " + n + " / 3");
            lblSelection.setForeground(n > 3 ? UiTheme.DANGER : UiTheme.TEXT);
        });
        right.add(new JScrollPane(lstReviewers), BorderLayout.CENTER);
        JPanel south = UiTheme.panel(new BorderLayout(4, 4));
        south.add(lblSelection, BorderLayout.NORTH);
        JButton assign = UiTheme.primary("Asignar y notificar por correo");
        assign.addActionListener(e -> assign());
        south.add(assign, BorderLayout.SOUTH);
        right.add(south, BorderLayout.SOUTH);

        root.add(left, BorderLayout.CENTER);
        root.add(right, BorderLayout.EAST);
        setContentPane(root);
        setSize(1050, 520);
        setLocationRelativeTo(null);

        app.events().subscribe(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                app.events().unsubscribe(AssignReviewersView.this);
            }
        });
        app.reviews().reviewers(this, list -> list.forEach(reviewersModel::addElement));
        load();
    }

    private void load() {
        app.reviews().pending(this, pager.page(), PAGE_SIZE, this::render);
    }

    private void render(PageDto<ReviewDto> page) {
        rows.clear();
        rows.addAll(page.items());
        model.setRowCount(0);
        for (ReviewDto r : page.items()) {
            model.addRow(new Object[]{r.statement(), r.authorLogin(), r.competence(), r.topic(),
                    UiTheme.date(r.submittedAt()), r.round()});
        }
        pager.update(page.page(), page.totalPages(), page.totalItems());
    }

    private void assign() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta.");
            return;
        }
        List<String> logins = lstReviewers.getSelectedValuesList().stream().map(UserDto::login).toList();
        String error = app.reviews().validateSelection(logins);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Asignar revisores", JOptionPane.WARNING_MESSAGE);
            return;
        }
        app.reviews().assign(this, rows.get(row).questionId(), logins, review -> {
            lstReviewers.clearSelection();
            JOptionPane.showMessageDialog(this, "Revisores asignados: " + String.join(", ", logins)
                    + ".\nLa pregunta pasó a \"En revisión\" y se enviaron las notificaciones.");
        });
    }

    @Override
    public void onChange(AppChange change) {
        if (change == AppChange.REVIEWS || change == AppChange.NOTIFICATIONS) {
            load();
        }
    }
}
