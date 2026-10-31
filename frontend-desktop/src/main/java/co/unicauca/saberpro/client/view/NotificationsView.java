package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.NotificationDto;
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

/** Bandeja de notificaciones generadas por notification-service a partir de los eventos. */
public class NotificationsView extends JFrame implements AppObserver {

    private final DesktopApp app;
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"", "Fecha", "Asunto", "Entrega correo"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextArea body = UiTheme.readOnly("");
    private final Pager pager = new Pager(p -> load());
    private final List<NotificationDto> rows = new ArrayList<>();

    public NotificationsView(DesktopApp app) {
        super("Notificaciones");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Mis notificaciones", app.session().user().login()), BorderLayout.NORTH);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setMaxWidth(30);
        table.getColumnModel().getColumn(2).setPreferredWidth(380);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelected();
            }
        });
        JPanel top = UiTheme.panel(new BorderLayout());
        top.add(new JScrollPane(table), BorderLayout.CENTER);
        top.add(pager, BorderLayout.SOUTH);
        JScrollPane bodyScroll = new JScrollPane(body);
        bodyScroll.setBorder(UiTheme.titled("Mensaje"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bodyScroll);
        split.setResizeWeight(0.55);
        split.setBorder(new EmptyBorder(8, 8, 8, 8));
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);
        setSize(820, 560);
        setLocationRelativeTo(null);
        app.events().subscribe(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                app.events().unsubscribe(NotificationsView.this);
            }
        });
        load();
    }

    private void load() {
        app.session().inbox(this, pager.page(), page -> {
            rows.clear();
            rows.addAll(page.items());
            model.setRowCount(0);
            for (NotificationDto n : page.items()) {
                model.addRow(new Object[]{n.read() ? "" : "●", UiTheme.date(n.createdAt()), n.subject(),
                        n.deliveryStatus()});
            }
            pager.update(page.page(), page.totalPages(), page.totalItems());
        });
    }

    private void showSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= rows.size()) {
            return;
        }
        NotificationDto n = rows.get(row);
        body.setText(n.body());
        body.setCaretPosition(0);
        if (!n.read()) {
            app.session().markRead(this, n.id());
        }
    }

    @Override
    public void onChange(AppChange change) {
        if (change == AppChange.NOTIFICATIONS) {
            int selected = table.getSelectedRow();
            load();
            if (selected >= 0) {
                SwingUtilities.invokeLater(() -> {
                    if (selected < table.getRowCount()) {
                        table.getSelectionModel().setSelectionInterval(selected, selected);
                    }
                });
            }
        }
    }
}
