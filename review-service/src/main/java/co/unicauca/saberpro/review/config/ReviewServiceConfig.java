package co.unicauca.saberpro.review.config;

import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.EventLogListener;
import co.unicauca.saberpro.common.messaging.RabbitEventForwarder;
import co.unicauca.saberpro.review.access.IReviewRepository;
import co.unicauca.saberpro.review.access.InMemoryReviewRepository;
import co.unicauca.saberpro.review.application.ReviewApplicationService;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Ensambla el review-service. */
@Configuration
public class ReviewServiceConfig {

    @Bean
    public IReviewRepository reviewRepository() {
        return new InMemoryReviewRepository();
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
    public ReviewApplicationService reviewApplicationService(IReviewRepository repository, DomainEventBus eventBus) {
        return new ReviewApplicationService(repository, eventBus);
    }
}
