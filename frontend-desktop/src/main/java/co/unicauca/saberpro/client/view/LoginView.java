package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Pantalla de inicio de sesion (consume user-service a traves del gateway). */
public class LoginView extends JFrame {

    private final DesktopApp app;
    private final JTextField txtLogin = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);

    public LoginView(DesktopApp app) {
        super("Banco de Preguntas Saber PRO - Ingreso");
        this.app = app;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Banco de Preguntas Saber PRO", "Iteración 2"), BorderLayout.NORTH);

        JPanel form = UiTheme.panel(new GridBagLayout());
        form.setBorder(new EmptyBorder(24, 32, 12, 32));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Usuario:"), c);
        c.gridx = 1; form.add(txtLogin, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Contraseña:"), c);
        c.gridx = 1; form.add(txtPassword, c);

        JButton btnLogin = UiTheme.primary("Ingresar");
        btnLogin.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);
        c.gridx = 1; c.gridy = 2; c.anchor = GridBagConstraints.EAST; form.add(btnLogin, c);
        root.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html><small>Gateway: " + app.gatewayUrl()
                + "<br>Pruebe: admin / Admin123! · autor1 / Autor123! · rev1 / Revisor123!</small></html>");
        hint.setBorder(new EmptyBorder(0, 16, 12, 16));
        root.add(hint, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }

    private void doLogin() {
        app.session().login(this, txtLogin.getText().trim(), new String(txtPassword.getPassword()), user -> {
            new MainView(app).setVisible(true);
            dispose();
        });
    }
}
