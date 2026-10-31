package co.unicauca.saberpro.frontend;

import co.unicauca.saberpro.frontend.ui.LoginFrame;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada del frontend de escritorio (Java Swing). Requiere el API
 * Gateway en ejecucion (por defecto http://localhost:8080).
 */
public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppContext context = new AppContext();
            new LoginFrame(context).setVisible(true);
        });
    }
}
