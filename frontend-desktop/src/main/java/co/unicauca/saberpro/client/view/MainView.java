package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;
import co.unicauca.saberpro.client.api.dto.UserDto;
import co.unicauca.saberpro.client.model.AppChange;
import co.unicauca.saberpro.client.model.AppObserver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

/** Menu principal segun el rol, con contador de notificaciones en vivo. */
public class MainView extends JFrame implements AppObserver {

    private static final int POLL_MILLIS = 5000;
    private final DesktopApp app;
    private final JButton btnNotifications = UiTheme.secondary("Notificaciones");
    private final Timer poller;

    public MainView(DesktopApp app) {
        super("Banco de Preguntas Saber PRO");
        this.app = app;
        UserDto user = app.session().user();
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Bienvenido/a, " + user.fullName(), user.roleLabel()), BorderLayout.NORTH);

        JPanel menu = UiTheme.panel(new GridLayout(0, 1, 0, 10));
        menu.setBorder(new EmptyBorder(24, 60, 12, 60));
        if (user.isAuthor()) {
            menu.add(option("Crear pregunta (HU01)", () -> new QuestionFormView(app, null)));
            menu.add(option("Mis preguntas (HU02 · HU03)", () -> new MyQuestionsView(app)));
        }
        if (user.isAdmin()) {
            menu.add(option("Asignar revisores (HU04)", () -> new AssignReviewersView(app)));
            menu.add(option("Registrar usuario", () -> new RegisterUserView(app)));
        }
        if (user.isReviewer()) {
            menu.add(option("Preguntas asignadas (HU05)", () -> new AssignedReviewsView(app)));
        }
        btnNotifications.addActionListener(e -> open(() -> new NotificationsView(app)));
        menu.add(btnNotifications);
        JButton logout = UiTheme.danger("Cerrar sesión");
        logout.addActionListener(e -> logout());
        menu.add(logout);
        root.add(menu, BorderLayout.CENTER);
        setContentPane(root);
        setSize(460, 360);
        setLocationRelativeTo(null);

        app.events().subscribe(this);
        poller = new Timer(POLL_MILLIS, e -> refreshBadge());
        poller.setInitialDelay(0);
        poller.start();
    }

    private JButton option(String text, Supplier<JFrame> factory) {
        JButton b = UiTheme.primary(text);
        b.addActionListener(e -> open(factory));
        return b;
    }

    private void open(Supplier<JFrame> factory) {
        factory.get().setVisible(true);
    }

    /** Consulta silenciosa en segundo plano; si el servidor no responde, simplemente no actualiza. */
    private void refreshBadge() {
        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() {
                return app.session().pollUnread();
            }

            @Override
            protected void done() {
                try {
                    long unread = get();
                    btnNotifications.setText(unread > 0 ? "Notificaciones (" + unread + " nuevas)" : "Notificaciones");
                } catch (Exception ignored) {
                    // sin conexion momentanea: se reintenta en el siguiente ciclo
                }
            }
        }.execute();
    }

    @Override
    public void onChange(AppChange change) {
        if (change == AppChange.NOTIFICATIONS) {
            refreshBadge();
        }
    }

    private void logout() {
        poller.stop();
        app.events().unsubscribe(this);
        app.session().logout();
        for (Window w : Window.getWindows()) {
            if (w != this) {
                w.dispose();
            }
        }
        new LoginView(app).setVisible(true);
        dispose();
    }
}
