package co.unicauca.saberpro.review.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentacion OpenAPI (Swagger). El servidor es relativo ("/") para que
 * "Try it out" funcione tanto desde este servicio como desde el Swagger del gateway.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("review-service").version("2.0.0")
                        .description("Revision por pares: asignacion de revisores y evaluaciones."))
                .servers(List.of(new Server().url("/")));
    }
}
