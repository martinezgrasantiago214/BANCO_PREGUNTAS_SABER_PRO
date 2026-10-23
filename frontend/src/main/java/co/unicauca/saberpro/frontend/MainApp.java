package co.unicauca.saberpro.presentation;

import javax.swing.SwingUtilities;

/** Punto de entrada de la aplicacion de escritorio (Java Swing). */
public class MainApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AppContext context = new AppContext();
            new LoginFrame(context).setVisible(true);
        });
    }
}
