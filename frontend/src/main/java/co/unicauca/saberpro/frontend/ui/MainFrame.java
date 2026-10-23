package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.Role;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Ventana principal / menu, cuyas opciones dependen del rol del usuario
 * autenticado.
 */
public class MainFrame extends JFrame {

    private final AppContext context;

    public MainFrame(AppContext context) {
        super("Banco de Preguntas Saber PRO");
        this.context = context;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 380);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Banco de Preguntas Saber PRO"), BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 40, 24, 40));
        UiTheme.applyBackground(panel);

        JLabel welcome = new JLabel("Bienvenido/a, " + context.getLoggedInUser().getFullName());
        welcome.setForeground(UiTheme.TEXT);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 15f));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(welcome);
        panel.add(Box.createVerticalStrut(4));

        JLabel roleLabel = new JLabel(context.getLoggedInUser().getRole().toString());
        roleLabel.setForeground(UiTheme.TEXT);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(roleLabel);
        panel.add(Box.createVerticalStrut(24));

        Role role = context.getLoggedInUser().getRole();

        if (role == Role.AUTOR_PREGUNTAS) {
            panel.add(centered(UiTheme.primaryButton("Crear pregunta"),
                    e -> new CreateQuestionFrame(context).setVisible(true)));
            panel.add(Box.createVerticalStrut(12));
            panel.add(centered(UiTheme.primaryButton("Mis preguntas"),
                    e -> new MyQuestionsFrame(context).setVisible(true)));
        } else if (role == Role.ADMINISTRADOR) {
            panel.add(centered(UiTheme.primaryButton("Asignar revisores"),
                    e -> new AssignReviewersFrame(context).setVisible(true)));
            panel.add(Box.createVerticalStrut(12));
            panel.add(centered(UiTheme.primaryButton("Registrar usuario"),
                    e -> new RegisterFrame(context).setVisible(true)));
        } else if (role == Role.REVISOR) {
            JLabel info = new JLabel("Aun no hay opciones disponibles para este rol.");
            info.setForeground(UiTheme.TEXT);
            info.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(info);
        }

        panel.add(Box.createVerticalStrut(24));
        panel.add(centered(UiTheme.secondaryButton("Cerrar sesion"), e -> logout()));

        root.add(panel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel centered(JButton button, java.awt.event.ActionListener listener) {
        button.addActionListener(listener);
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(wrapper);
        wrapper.add(button);
        wrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        return wrapper;
    }

    private void logout() {
        context.setLoggedInUser(null);
        new LoginFrame(context).setVisible(true);
        dispose();
    }
}
