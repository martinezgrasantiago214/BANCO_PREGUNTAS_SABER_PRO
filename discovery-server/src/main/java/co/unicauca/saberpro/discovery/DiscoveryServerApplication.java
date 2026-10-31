package co.unicauca.saberpro.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry: cada instancia de microservicio se registra aqui y el
 * gateway / los clientes REST la descubren por nombre logico. Permite
 * agregar replicas sin reconfigurar a nadie (escalabilidad horizontal).
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServerApplication.class, args);
    }
}
