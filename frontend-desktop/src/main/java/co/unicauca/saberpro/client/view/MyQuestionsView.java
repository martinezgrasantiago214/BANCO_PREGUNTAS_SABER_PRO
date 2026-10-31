package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.CatalogItemDto;
import co.unicauca.saberpro.client.api.dto.PageDto;
import co.unicauca.saberpro.client.api.dto.QuestionDto;
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

/**
 * HU03: listado paginado y filtrado de las preguntas del autor.
 * HU02: estados con color y accion "Enviar a revisión".
 * Es Observer: se refresca sola cuando otra ventana o un evento del backend
 * cambia el estado de las preguntas.
 */
public class MyQuestionsView extends JFrame implements AppObserver {

    private static final int PAGE_SIZE = 8;
    private final DesktopApp app;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Pregunta directa", "Competencia", "Tema", "Dificultad", "Actualizada", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JComboBox<CatalogItemDto> cmbState = new JComboBox<>();
    private final JTextField txtTopic = new JTextField(10);
    private final JTextField txtKeyword = new JTextField(12);
    private final Pager pager = new Pager(p -> load());
    private final JPanel legendHolder = UiTheme.panel(new BorderLayout());
    private final List<QuestionDto> rows = new ArrayList<>();

    public MyQuestionsView(DesktopApp app) {
        super("Mis preguntas");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Mis preguntas", app.session().user().login()), BorderLayout.NORTH);

        JPanel center = UiTheme.panel(new BorderLayout(6, 6));
        center.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel filters = UiTheme.panel(new FlowLayout(FlowLayout.LEFT));
        cmbState.addItem(new CatalogItemDto(null, "(Todos los estados)", null));
        filters.add(new JLabel("Estado:"));
        filters.add(cmbState);
        filters.add(new JLabel("Tema:"));
        filters.add(txtTopic);
        filters.add(new JLabel("Palabra clave:"));
        filters.add(txtKeyword);
        JButton search = UiTheme.secondary("Buscar");
        search.addActionListener(e -> { pager.reset(); load(); });
        filters.add(search);
        JPanel top = UiTheme.panel(new BorderLayout());
        top.add(filters, BorderLayout.NORTH);
        top.add(legendHolder, BorderLayout.SOUTH);
        center.add(top, BorderLayout.NORTH);

        table.setRowHeight(26);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(340);
        table.getColumnModel().getColumn(5).setCellRenderer(new UiTheme.StateRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetail();
                }
            }
        });
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        center.add(pager, BorderLayout.SOUTH);
        root.add(center, BorderLayout.CENTER);

        JPanel actions = UiTheme.panel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(button(UiTheme.primary("Nueva pregunta"), () -> new QuestionFormView(app, null).setVisible(true)));
        actions.add(button(UiTheme.secondary("Ver detalle y observaciones"), this::showDetail));
        actions.add(button(UiTheme.secondary("Editar"), this::edit));
        actions.add(button(UiTheme.primary("Enviar a revisión"), this::submit));
        actions.add(button(UiTheme.danger("Eliminar"), this::delete));
        root.add(actions, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(1000, 600);
        setLocationRelativeTo(null);

        app.events().subscribe(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                app.events().unsubscribe(MyQuestionsView.this);
            }
        });
        app.author().states(this, states -> {
            states.forEach(cmbState::addItem);
            legendHolder.add(UiTheme.legend(states));
            legendHolder.revalidate();
        });
        load();
    }

    private JButton button(JButton b, Runnable action) {
        b.addActionListener(e -> action.run());
        return b;
    }

    private void load() {
        CatalogItemDto state = (CatalogItemDto) cmbState.getSelectedItem();
        app.author().search(this, state == null ? null : state.code(), txtTopic.getText().trim(),
                txtKeyword.getText().trim(), pager.page(), PAGE_SIZE, this::render);
    }

    private void render(PageDto<QuestionDto> page) {
        rows.clear();
        rows.addAll(page.items());
        model.setRowCount(0);
        for (QuestionDto q : page.items()) {
            model.addRow(new Object[]{q.statement(), q.competence(), q.topic(), q.difficulty(),
                    UiTheme.date(q.updatedAt()), new UiTheme.StateCell(q.stateLabel(), q.stateColor())});
        }
        pager.update(page.page(), page.totalPages(), page.totalItems());
    }

    private QuestionDto selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una pregunta de la tabla.");
            return null;
        }
        return rows.get(row);
    }

    private void showDetail() {
        QuestionDto q = selected();
        if (q != null) {
            app.author().detail(this, q.id(), d -> QuestionDetailDialog.show(this, d.question(), d.observations()));
        }
    }

    private void edit() {
        QuestionDto q = selected();
        if (q == null) {
            return;
        }
        if (!q.editable()) {
            JOptionPane.showMessageDialog(this, "Solo se pueden editar preguntas en Borrador o Rechazada.");
            return;
        }
        new QuestionFormView(app, q).setVisible(true);
    }

    private void submit() {
        QuestionDto q = selected();
        if (q != null && JOptionPane.showConfirmDialog(this,
                "¿Enviar la pregunta a revisión? Ya no podrá editarla hasta conocer el resultado.",
                "Enviar a revisión", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            app.author().submitForReview(this, q.id(), updated -> JOptionPane.showMessageDialog(this,
                    "La pregunta quedó \"" + updated.stateLabel() + "\". Se notificó al administrador."));
        }
    }

    private void delete() {
        QuestionDto q = selected();
        if (q != null && JOptionPane.showConfirmDialog(this, "¿Eliminar definitivamente la pregunta?",
                "Eliminar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            app.author().delete(this, q.id(), () -> { });
        }
    }

    @Override
    public void onChange(AppChange change) {
        if (change == AppChange.QUESTIONS) {
            load();
        }
    }
}
