package co.unicauca.saberpro.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway: el frontend solo conoce esta URL (http://localhost:8080).
 * Enruta a cada microservicio por nombre logico (lb://) con balanceo de
 * carga entre replicas, y publica un Swagger UI que agrupa las APIs.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
