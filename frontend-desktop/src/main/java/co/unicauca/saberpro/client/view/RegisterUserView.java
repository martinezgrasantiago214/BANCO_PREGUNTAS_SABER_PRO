package co.unicauca.saberpro.client.view;

import co.unicauca.saberpro.client.DesktopApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** El administrador registra autores, revisores u otros administradores. */
public class RegisterUserView extends JFrame {

    public RegisterUserView(DesktopApp app) {
        super("Registrar usuario");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JTextField login = new JTextField(18);
        JTextField name = new JTextField(18);
        JTextField email = new JTextField(18);
        JComboBox<String> role = new JComboBox<>(new String[]{"AUTOR_PREGUNTAS", "REVISOR", "ADMINISTRADOR"});
        JPasswordField password = new JPasswordField(18);

        JPanel form = UiTheme.panel(new GridLayout(0, 2, 8, 8));
        form.setBorder(new EmptyBorder(16, 20, 16, 20));
        form.add(new JLabel("Login:"));
        form.add(login);
        form.add(new JLabel("Nombre completo:"));
        form.add(name);
        form.add(new JLabel("Correo:"));
        form.add(email);
        form.add(new JLabel("Rol:"));
        form.add(role);
        form.add(new JLabel("Contraseña:"));
        form.add(password);
        JButton save = UiTheme.primary("Registrar");
        save.addActionListener(e -> app.session().register(this, login.getText().trim(), name.getText().trim(),
                email.getText().trim(), (String) role.getSelectedItem(), new String(password.getPassword()), u -> {
                    JOptionPane.showMessageDialog(this, "Usuario " + u.login() + " registrado.");
                    dispose();
                }));
        form.add(new JLabel());
        form.add(save);

        JPanel root = UiTheme.panel(new BorderLayout());
        root.add(UiTheme.header("Registrar usuario", null), BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setLocationRelativeTo(null);
    }
}
