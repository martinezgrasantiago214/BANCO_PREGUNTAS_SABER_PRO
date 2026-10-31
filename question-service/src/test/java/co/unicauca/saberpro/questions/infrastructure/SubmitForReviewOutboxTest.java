package co.unicauca.saberpro.questions.infrastructure;

import co.unicauca.saberpro.events.EventTopology;
import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.outbox.OutboxMessage;
import co.unicauca.saberpro.outbox.OutboxMessageSender;
import co.unicauca.saberpro.outbox.OutboxRelay;
import co.unicauca.saberpro.outbox.OutboxRepository;
import co.unicauca.saberpro.questions.application.QuestionService;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.testdoubles.QuestionFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Levanta question-service completo (JPA real sobre H2, sin Eureka ni RabbitMQ)
 * y verifica que HU02 deja el cambio de estado y su evento en una sola transaccion.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:questions-it;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "saberpro.outbox.enabled=false"
})
class SubmitForReviewOutboxTest {

    @Autowired QuestionService questionService;
    @Autowired OutboxRepository outbox;
    @Autowired OutboxRelay relay;
    @Autowired FakeBroker broker;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        broker.sent.clear();
    }

    @Test
    void elServicioDeAplicacionQuedaEnvueltoEnUnaTransaccion() {
        assertThat(AopUtils.isAopProxy(questionService)).isTrue();
    }

    @Test
    void enviarARevisionGuardaElEstadoYElEventoJuntos() {
        Question created = questionService.create(QuestionFixtures.valid("autor1"));
        assertThat(outbox.count()).isZero();

        questionService.submitForReview(created.getId(), "autor1");

        assertThat(questionService.findById(created.getId()).getStateType())
                .isEqualTo(QuestionStateType.PENDIENTE_REVISION);
        List<OutboxMessage> pending = outbox.findAll();
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getRoutingKey()).isEqualTo(EventTopology.QUESTION_SUBMITTED);
        assertThat(pending.get(0).getAggregateId()).isEqualTo(created.getId());

        assertThat(relay.relayPending()).isEqualTo(1);
        assertThat(broker.sent).singleElement()
                .isInstanceOfSatisfying(QuestionSubmittedForReviewEvent.class,
                        e -> assertThat(e.questionId()).isEqualTo(created.getId()));
    }

    @Test
    void unaTransicionInvalidaNoDejaEventosHuerfanos() {
        Question created = questionService.create(QuestionFixtures.valid("autor1"));
        questionService.submitForReview(created.getId(), "autor1");
        outbox.deleteAll();

        assertThatThrownBy(() -> questionService.submitForReview(created.getId(), "autor1"));
        assertThatThrownBy(() -> questionService.submitForReview(created.getId(), "otro-autor"));

        assertThat(outbox.count()).isZero();
    }

    @TestConfiguration
    static class BrokerConfig {
        @Bean
        FakeBroker fakeBroker() {
            return new FakeBroker();
        }
    }

    static class FakeBroker implements OutboxMessageSender {
        final List<Object> sent = new ArrayList<>();

        @Override
        public void send(String routingKey, Object event) {
            sent.add(event);
        }
    }
}
