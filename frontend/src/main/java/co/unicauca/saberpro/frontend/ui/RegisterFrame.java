package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.domain.Role;
import co.unicauca.saberpro.domain.UserStatus;
import co.unicauca.saberpro.service.OperationResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Ventana de registro de nuevos usuarios (autores, administradores o revisores). */
public class RegisterFrame extends JFrame {

    private final AppContext context;
    private final JTextField txtLogin = new JTextField(18);
    private final JTextField txtFullName = new JTextField(18);
    private final JTextField txtEmail = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);
    private final JComboBox<Role> cmbRole = new JComboBox<>(Role.values());

    public RegisterFrame(AppContext context) {
        super("Registro de usuario");
        this.context = context;
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(440, 420);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        UiTheme.applyBackground(root);
        root.add(UiTheme.header("Registro de usuario"), BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(16, 16, 16, 16));
        UiTheme.applyBackground(panel);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        c.gridx = 0; c.gridy = row; panel.add(new JLabel("Usuario:"), c);
        c.gridx = 1; panel.add(txtLogin, c); row++;

        c.gridx = 0; c.gridy = row; panel.add(new JLabel("Nombre completo:"), c);
        c.gridx = 1; panel.add(txtFullName, c); row++;

        c.gridx = 0; c.gridy = row; panel.add(new JLabel("Correo:"), c);
        c.gridx = 1; panel.add(txtEmail, c); row++;

        c.gridx = 0; c.gridy = row; panel.add(new JLabel("Rol:"), c);
        c.gridx = 1; panel.add(cmbRole, c); row++;

        c.gridx = 0; c.gridy = row; panel.add(new JLabel("Contrasena:"), c);
        c.gridx = 1; panel.add(txtPassword, c); row++;

        JLabel hint = new JLabel("<html><i>Minimo 6 caracteres, una mayuscula, "
                + "un digito y un caracter especial.</i></html>");
        hint.setForeground(UiTheme.TEXT);
        hint.setFont(hint.getFont().deriveFont(11f));
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        panel.add(hint, c); row++;

        JButton btnRegister = UiTheme.primaryButton("Registrar");
        btnRegister.addActionListener(e -> onRegister());
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        panel.add(btnRegister, c);

        root.add(panel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void onRegister() {
        OperationResult result = context.getUserService().register(
                txtLogin.getText().trim(),
                txtFullName.getText().trim(),
                txtEmail.getText().trim(),
                (Role) cmbRole.getSelectedItem(),
                UserStatus.ACTIVO,
                new String(txtPassword.getPassword())
        );

        if (result.isSuccess()) {
            JOptionPane.showMessageDialog(this, "Usuario registrado correctamente. Ya puede iniciar sesion.",
                    "Registro exitoso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, result.getErrorsAsText(),
                    "No fue posible registrar el usuario", JOptionPane.WARNING_MESSAGE);
        }
    }
}
