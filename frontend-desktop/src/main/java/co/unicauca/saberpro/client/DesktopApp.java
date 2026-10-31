package co.unicauca.saberpro.client;

import co.unicauca.saberpro.client.api.SaberProApiFacade;
import co.unicauca.saberpro.client.controller.AuthorController;
import co.unicauca.saberpro.client.controller.ReviewController;
import co.unicauca.saberpro.client.controller.SessionController;
import co.unicauca.saberpro.client.model.AppEvents;
import co.unicauca.saberpro.client.view.LoginView;

import javax.swing.*;

/**
 * Punto de entrada del frontend de escritorio. Solo conoce la URL del API
 * Gateway (por defecto http://localhost:8080); configurable con
 * -Dsaberpro.api=... o la variable de entorno SABERPRO_API.
 *
 * Arma el micro-patron MVC: Modelo (AppEvents + DTOs), Vistas (Swing) y
 * Controladores, todos apoyados en la Fachada de la API.
 */
public class DesktopApp {

    private final String gatewayUrl;
    private final AppEvents events = new AppEvents();
    private final SessionController session;
    private final AuthorController author;
    private final ReviewController reviews;

    public DesktopApp(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
        SaberProApiFacade api = new SaberProApiFacade(gatewayUrl);
        this.session = new SessionController(api, events);
        this.author = new AuthorController(api, events);
        this.reviews = new ReviewController(api, events);
    }

    public static void main(String[] args) {
        String url = System.getProperty("saberpro.api",
                System.getenv().getOrDefault("SABERPRO_API", "http://localhost:8080"));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // se usa el look and feel por defecto
            }
            new LoginView(new DesktopApp(url)).setVisible(true);
        });
    }

    public String gatewayUrl() { return gatewayUrl; }
    public AppEvents events() { return events; }
    public SessionController session() { return session; }
    public AuthorController author() { return author; }
    public ReviewController reviews() { return reviews; }
}
