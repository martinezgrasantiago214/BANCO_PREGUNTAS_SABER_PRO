package co.unicauca.saberpro.review.infrastructure.config;

import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventLogListener;
import co.unicauca.saberpro.common.messaging.RabbitEventForwarder;
import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase;
import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase;
import co.unicauca.saberpro.review.application.port.in.RegisterSubmissionUseCase;
import co.unicauca.saberpro.review.application.port.in.ReviewQueries;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.application.port.out.ReviewerDirectory;
import co.unicauca.saberpro.review.application.service.AssignReviewersService;
import co.unicauca.saberpro.review.application.service.EvaluateQuestionService;
import co.unicauca.saberpro.review.application.service.RegisterSubmissionService;
import co.unicauca.saberpro.review.application.service.ReviewQueryService;
import co.unicauca.saberpro.review.infrastructure.adapter.out.directory.UserServiceReviewerDirectory;
import co.unicauca.saberpro.review.infrastructure.adapter.out.events.DomainEventBusReviewPublisher;
import co.unicauca.saberpro.review.infrastructure.adapter.out.persistence.SqliteQuestionReviewRepository;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.time.Clock;

/**
 * Raiz de composicion (arquitectura hexagonal): el unico lugar que conoce a
 * la vez los puertos y sus adaptadores. Los casos de uso y el dominio son
 * clases Java comunes, sin anotaciones de Spring; aqui se conectan.
 *
 * <pre>
 *  adaptadores de entrada         nucleo (aplicacion + dominio)          adaptadores de salida
 *  ReviewController  ──┐                                          ┌──> SqliteQuestionReviewRepository
 *  ReviewEventsConsumer┴──> puertos in ──> casos de uso ──> puertos out ┼──> UserServiceReviewerDirectory
 *                                         QuestionReview (agregado)     └──> DomainEventBusReviewPublisher
 * </pre>
 */
@Configuration
public class ReviewServiceConfig {

    // ---- adaptadores de salida ----

    /** Base de datos propia del review-service (en Docker, en el volumen review-data). */
    @Bean(destroyMethod = "close")
    public SqliteQuestionReviewRepository questionReviewRepository(@Value("${saberpro.data-dir}") String dataDir) {
        File dir = new File(dataDir);
        dir.mkdirs();
        return new SqliteQuestionReviewRepository("jdbc:sqlite:" + new File(dir, "review-service.db").getAbsolutePath());
    }

    /** Directorio de usuarios: el user-service. El token de servicio (Keycloak) se agrega en #49. */
    @Bean
    public ReviewerDirectory reviewerDirectory(@Value("${saberpro.user-service-url}") String userServiceUrl) {
        return new UserServiceReviewerDirectory(RestClient.builder().baseUrl(userServiceUrl).build());
    }

    /** Sujeto del patron Observer con sus observadores: el log y el reenvio al broker. */
    @Bean
    public DomainEventBus domainEventBus(AmqpTemplate amqpTemplate) {
        DomainEventBus bus = new DomainEventBus();
        bus.subscribe(new EventLogListener());
        bus.subscribe(new RabbitEventForwarder(amqpTemplate));
        return bus;
    }

    @Bean
    public ReviewEventPublisher reviewEventPublisher(DomainEventBus bus) {
        return new DomainEventBusReviewPublisher(bus);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // ---- casos de uso (puertos de entrada) ----

    @Bean
    public RegisterSubmissionUseCase registerSubmissionUseCase(QuestionReviewRepository repository,
                                                               ReviewEventPublisher publisher) {
        return new RegisterSubmissionService(repository, publisher);
    }

    @Bean
    public AssignReviewersUseCase assignReviewersUseCase(QuestionReviewRepository repository,
                                                         ReviewerDirectory directory,
                                                         ReviewEventPublisher publisher, Clock clock) {
        return new AssignReviewersService(repository, directory, publisher, clock);
    }

    @Bean
    public EvaluateQuestionUseCase evaluateQuestionUseCase(QuestionReviewRepository repository,
                                                           ReviewEventPublisher publisher, Clock clock) {
        return new EvaluateQuestionService(repository, publisher, clock);
    }

    @Bean
    public ReviewQueries reviewQueries(QuestionReviewRepository repository) {
        return new ReviewQueryService(repository);
    }
}
