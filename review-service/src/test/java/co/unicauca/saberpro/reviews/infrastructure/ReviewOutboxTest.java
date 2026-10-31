package co.unicauca.saberpro.reviews.infrastructure;

import co.unicauca.saberpro.events.EventTopology;
import co.unicauca.saberpro.events.ReviewCompletedEvent;
import co.unicauca.saberpro.outbox.OutboxMessage;
import co.unicauca.saberpro.outbox.OutboxMessageSender;
import co.unicauca.saberpro.outbox.OutboxRelay;
import co.unicauca.saberpro.outbox.OutboxRepository;
import co.unicauca.saberpro.reviews.application.EvaluationCommand;
import co.unicauca.saberpro.reviews.application.ReviewService;
import co.unicauca.saberpro.reviews.application.port.UserDirectoryPort;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import co.unicauca.saberpro.reviews.testdoubles.FakeUserDirectory;
import co.unicauca.saberpro.reviews.testdoubles.ReviewFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Levanta review-service completo (JPA real sobre H2, sin Eureka, RabbitMQ ni
 * user-service) y verifica que HU04 y HU05 escriben sus eventos de forma atomica.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:reviews-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "saberpro.outbox.enabled=false"
})
class ReviewOutboxTest {

    @Autowired ReviewService reviewService;
    @Autowired OutboxRepository outbox;
    @Autowired OutboxRelay relay;
    @Autowired FakeBroker broker;

    private String questionId;

    @BeforeEach
    void prepare() {
        outbox.deleteAll();
        broker.routingKeys.clear();
        broker.sent.clear();
        questionId = UUID.randomUUID().toString();
        reviewService.registerSubmission(questionId, "autor1", "¿Qué estilo favorece la escalabilidad?",
                "Diseño de software", "Arquitectura");
    }

    @Test
    void asignarRevisoresDejaElEventoEnLaOutbox() {
        reviewService.assignReviewers(questionId, List.of("rev1"), "admin");

        assertThat(routingKeysInOutbox()).containsExactly(EventTopology.REVIEWERS_ASSIGNED);
    }

    @Test
    void laEvaluacionFinalEmiteSusTresEventosJuntosYEnOrden() {
        reviewService.assignReviewers(questionId, List.of("rev1"), "admin");
        outbox.deleteAll();

        reviewService.submitEvaluation(questionId, "rev1",
                new EvaluationCommand(Verdict.APROBADA, ReviewFixtures.allScores(5), "Muy clara"));

        List<String> expected = List.of(EventTopology.EVALUATION_SUBMITTED, EventTopology.OBSERVATION_ADDED,
                EventTopology.REVIEW_COMPLETED);
        assertThat(routingKeysInOutbox()).containsExactlyElementsOf(expected);

        assertThat(relay.relayPending()).isEqualTo(3);
        assertThat(broker.routingKeys).containsExactlyElementsOf(expected);
        assertThat(broker.sent.get(2)).isInstanceOfSatisfying(ReviewCompletedEvent.class,
                e -> assertThat(e.decision()).isEqualTo("APROBADA"));
    }

    @Test
    void siElRevisorNoEstaAsignadoNoSeGuardaNingunEvento() {
        reviewService.assignReviewers(questionId, List.of("rev1"), "admin");
        outbox.deleteAll();

        assertThatThrownBy(() -> reviewService.submitEvaluation(questionId, "rev2",
                new EvaluationCommand(Verdict.APROBADA, ReviewFixtures.allScores(5), "Hola")));

        assertThat(outbox.count()).isZero();
    }

    private List<String> routingKeysInOutbox() {
        return outbox.findAll(Sort.by("id")).stream().map(OutboxMessage::getRoutingKey).toList();
    }

    @TestConfiguration
    static class TestDoubles {
        @Bean
        FakeBroker fakeBroker() {
            return new FakeBroker();
        }

        @Bean
        @Primary
        UserDirectoryPort fakeUserDirectory() {
            return new FakeUserDirectory();
        }
    }

    static class FakeBroker implements OutboxMessageSender {
        final List<String> routingKeys = new ArrayList<>();
        final List<Object> sent = new ArrayList<>();

        @Override
        public void send(String routingKey, Object event) {
            routingKeys.add(routingKey);
            sent.add(event);
        }
    }
}
