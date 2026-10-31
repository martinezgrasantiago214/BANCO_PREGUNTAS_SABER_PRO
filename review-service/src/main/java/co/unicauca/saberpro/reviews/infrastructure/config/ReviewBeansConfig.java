package co.unicauca.saberpro.reviews.infrastructure.config;

import co.unicauca.saberpro.reviews.application.ReviewService;
import co.unicauca.saberpro.reviews.application.port.ReviewEventPublisherPort;
import co.unicauca.saberpro.reviews.application.port.ReviewRepositoryPort;
import co.unicauca.saberpro.reviews.application.port.UserDirectoryPort;
import co.unicauca.saberpro.reviews.domain.policy.DecisionPolicy;
import co.unicauca.saberpro.reviews.domain.policy.MajorityPolicy;
import co.unicauca.saberpro.reviews.domain.policy.UnanimousApprovalPolicy;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class ReviewBeansConfig {

    /** Strategy seleccionada por configuracion: review.decision-policy=UNANIMOUS | MAJORITY. */
    @Bean
    DecisionPolicy decisionPolicy(@Value("${review.decision-policy:UNANIMOUS}") String policy) {
        return "MAJORITY".equalsIgnoreCase(policy) ? new MajorityPolicy() : new UnanimousApprovalPolicy();
    }

    @Bean
    ReviewService reviewService(ReviewRepositoryPort repository, ReviewEventPublisherPort publisher,
                                UserDirectoryPort userDirectory, DecisionPolicy policy) {
        return new ReviewService(repository, publisher, userDirectory, policy);
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    OpenAPI reviewServiceOpenApi() {
        return new OpenAPI()
                .info(new Info().title("review-service").version("2.0.0")
                        .description("Contexto Revisión por Pares: asignación de revisores, rúbrica y observaciones."))
                .servers(List.of(new Server().url("/").description("Vía API Gateway")));
    }
}
