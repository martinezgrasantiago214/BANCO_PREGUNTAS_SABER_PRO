package co.unicauca.saberpro.review.config;

import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventLogListener;
import co.unicauca.saberpro.common.messaging.RabbitEventForwarder;
import co.unicauca.saberpro.review.access.IReviewRepository;
import co.unicauca.saberpro.review.access.SQLiteReviewRepository;
import co.unicauca.saberpro.review.application.ReviewApplicationService;
import co.unicauca.saberpro.review.directory.UserRoleDirectory;
import co.unicauca.saberpro.review.directory.UserServiceRoleClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/** Ensambla el review-service. */
@Configuration
public class ReviewServiceConfig {

    /** PSP-33: base de datos propia del review-service. */
    @Bean
    public IReviewRepository reviewRepository(@Value("${saberpro.data-dir}") String dataDir) {
        File dir = new File(dataDir);
        dir.mkdirs();
        return new SQLiteReviewRepository("jdbc:sqlite:" + new File(dir, "review-service.db").getAbsolutePath());
    }

    @Bean
    public UserRoleDirectory userRoleDirectory(@Value("${saberpro.user-service-url}") String userServiceUrl) {
        return new UserServiceRoleClient(userServiceUrl);
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
    public ReviewApplicationService reviewApplicationService(IReviewRepository repository, DomainEventBus eventBus,
                                                             UserRoleDirectory userRoleDirectory) {
        return new ReviewApplicationService(repository, eventBus, userRoleDirectory);
    }
}
