package co.unicauca.saberpro.frontend;

import co.unicauca.saberpro.frontend.client.ApiClient;
import co.unicauca.saberpro.frontend.client.RestApis.RestQuestionApi;
import co.unicauca.saberpro.frontend.client.RestApis.RestReviewApi;
import co.unicauca.saberpro.frontend.client.RestApis.RestUserApi;
import co.unicauca.saberpro.frontend.facade.BancoPreguntasFacade;

/**
 * Contenedor de dependencias del frontend: se construye una vez al arrancar
 * y se comparte entre todas las ventanas. Las ventanas solo usan la fachada.
 *
 * La URL del API Gateway se puede cambiar con -Dsaberpro.gateway.url=...
 */
public final class AppContext {

    public static final String DEFAULT_GATEWAY_URL = "http://localhost:8080";

    private final BancoPreguntasFacade facade;

    public AppContext() {
        this(System.getProperty("saberpro.gateway.url", DEFAULT_GATEWAY_URL));
    }

    public AppContext(String gatewayUrl) {
        ApiClient client = new ApiClient(gatewayUrl);
        this.facade = new BancoPreguntasFacade(new RestUserApi(client), new RestQuestionApi(client),
                new RestReviewApi(client));
    }

    public AppContext(BancoPreguntasFacade facade) {
        this.facade = facade;
    }

    public BancoPreguntasFacade getFacade() {
        return facade;
    }
}
