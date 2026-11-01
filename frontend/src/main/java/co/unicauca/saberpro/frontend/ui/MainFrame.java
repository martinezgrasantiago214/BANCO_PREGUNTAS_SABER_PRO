package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.AppContext;
import co.unicauca.saberpro.frontend.model.Models.UserView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Ventana principal / menu, cuyas opciones dependen del rol del usuario
 * autenticado. Las pantallas que aun no existen muestran el issue de Jira que
 * las implementa.
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
        setSize(520, 400);
        setLocationRelativeTo(null);

        UserView user = context.getFacade().getCurrentUser();

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Banco de Preguntas Saber PRO"), BorderLayout.NORTH);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 40, 24, 40));
        UiTheme.applyBackground(panel);

        JLabel welcome = new JLabel("Bienvenido/a, " + user.fullName());
        welcome.setForeground(UiTheme.TEXT);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 15f));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(welcome);
        panel.add(Box.createVerticalStrut(4));

        JLabel roleLabel = new JLabel(user.role().toString());
        roleLabel.setForeground(UiTheme.TEXT);
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(roleLabel);
        panel.add(Box.createVerticalStrut(24));

        Role role = user.role();
        if (role == Role.AUTOR_PREGUNTAS) {
            panel.add(centered(UiTheme.primaryButton("Crear pregunta"),
                    e -> new CreateQuestionFrame(context).setVisible(true)));
            panel.add(Box.createVerticalStrut(12));
            // TODO(PSP-22, PSP-24..28): pantalla "Mis preguntas" (listado con colores, filtros, paginacion,
            //  enviar a revision y editar) usando facade.searchMyQuestions / submitForReview / updateQuestion.
            panel.add(centered(UiTheme.primaryButton("Mis preguntas"),
                    e -> Dialogs.showPendingScreen(this, "Mis preguntas", "PSP-22, PSP-24 a PSP-28")));
        } else if (role == Role.ADMINISTRADOR) {
            // TODO(PSP-30..32): pantalla "Asignar revisores" usando facade.listPendingQuestions,
            //  listReviewers y assignReviewers.
            panel.add(centered(UiTheme.primaryButton("Asignar revisores"),
                    e -> Dialogs.showPendingScreen(this, "Asignar revisores", "PSP-30 a PSP-32")));
            panel.add(Box.createVerticalStrut(12));
            panel.add(centered(UiTheme.primaryButton("Registrar usuario"),
                    e -> new RegisterFrame(context).setVisible(true)));
        } else if (role == Role.REVISOR) {
            panel.add(centered(UiTheme.primaryButton("Preguntas asignadas"),
                    e -> new AssignedQuestionsFrame(context).setVisible(true)));
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
        context.getFacade().logout();
        new LoginFrame(context).setVisible(true);
        dispose();
    }
}
