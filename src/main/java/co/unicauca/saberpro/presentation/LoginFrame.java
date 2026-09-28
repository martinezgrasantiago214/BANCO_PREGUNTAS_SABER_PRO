package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

/** Ventana de autenticacion. */
public class LoginFrame extends JFrame {

    private final AppContext context;
    private final JTextField txtLogin = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);

    public LoginFrame(AppContext context) {
        super("Banco de Preguntas Saber PRO - Iniciar sesion");
        this.context = context;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 320);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Banco de Preguntas Saber PRO"), BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        UiTheme.applyBackground(panel);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0; c.gridx = 0; panel.add(new JLabel("Usuario:"), c);
        c.gridx = 1; panel.add(txtLogin, c);

        c.gridy = 1; c.gridx = 0; panel.add(new JLabel("Contrasena:"), c);
        c.gridx = 1; panel.add(txtPassword, c);

        JButton btnLogin = UiTheme.primaryButton("Ingresar");
        btnLogin.addActionListener(e -> onLogin());
        JButton btnRegister = UiTheme.secondaryButton("Registrarse");
        btnRegister.addActionListener(e -> new RegisterFrame(context).setVisible(true));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        UiTheme.applyBackground(buttons);
        buttons.add(btnLogin);
        buttons.add(btnRegister);
        c.gridy = 2; c.gridx = 0; c.gridwidth = 2;
        panel.add(buttons, c);

    

        root.add(panel, BorderLayout.CENTER);
        setContentPane(root);
        getRootPane().setDefaultButton(btnLogin);
    }

    private void onLogin() {
        String login = txtLogin.getText().trim();
        String password = new String(txtPassword.getPassword());

        Optional<User> result = context.getUserService().login(login, password);
        if (result.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario o contrasena incorrectos, o usuario inactivo.",
                    "Error de autenticacion", JOptionPane.ERROR_MESSAGE);
            return;
        }

        context.setLoggedInUser(result.get());
        new MainFrame(context).setVisible(true);
        dispose();
    }
}
