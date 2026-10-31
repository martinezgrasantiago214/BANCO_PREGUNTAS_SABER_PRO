package co.unicauca.saberpro.outbox;

import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.outbox.OutboxTestApplication.FakeUseCase;
import co.unicauca.saberpro.outbox.OutboxTestApplication.MutableClock;
import co.unicauca.saberpro.outbox.OutboxTestApplication.RecordingSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.Duration;

import static co.unicauca.saberpro.outbox.OutboxTestApplication.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = OutboxTestApplication.class)
class OutboxIntegrationTest {

    @Autowired FakeUseCase useCase;
    @Autowired OutboxEventWriter writer;
    @Autowired OutboxRelay relay;
    @Autowired OutboxRepository repository;
    @Autowired RecordingSender broker;
    @Autowired MutableClock clock;
    @Autowired OutboxHealthIndicator health;

    @BeforeEach
    void reset() {
        repository.deleteAll();
        broker.sent.clear();
        broker.routingKeys.clear();
        broker.down = false;
    }

    @Test
    void elEventoSeGuardaEnLaMismaTransaccionYElRelayLoEntrega() {
        useCase.submit("e-1", "q-1");

        OutboxMessage pending = repository.findByEventId("e-1").orElseThrow();
        assertThat(pending.isPublished()).isFalse();
        assertThat(pending.getAggregateId()).isEqualTo("q-1");
        assertThat(broker.sent).isEmpty();

        assertThat(relay.relayPending()).isEqualTo(1);

        assertThat(broker.routingKeys).containsExactly("question.submitted");
        assertThat(broker.sent).containsExactly(event("e-1", "q-1"));
        assertThat(broker.sent.get(0)).isInstanceOf(QuestionSubmittedForReviewEvent.class);
        assertThat(repository.findByEventId("e-1").orElseThrow().isPublished()).isTrue();
        assertThat(relay.relayPending()).isZero();
    }

    @Test
    void siElCasoDeUsoFallaElEventoTambienSeDescarta() {
        assertThatThrownBy(() -> useCase.submitThenFail("e-2", "q-2")).isInstanceOf(IllegalStateException.class);

        assertThat(repository.findByEventId("e-2")).isEmpty();
        assertThat(relay.relayPending()).isZero();
    }

    @Test
    void noSePuedeEncolarFueraDeUnaTransaccion() {
        assertThatThrownBy(() -> writer.enqueue("question.submitted", event("e-3", "q-3")))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(repository.count()).isZero();
    }

    @Test
    void conElBrokerCaidoLosEventosEsperanYLuegoSalenEnOrden() {
        // Mismo instante: el orden lo define la secuencia, no la hora.
        useCase.submit("e-a", "q-a");
        useCase.submit("e-b", "q-b");
        broker.down = true;

        assertThat(relay.relayPending()).isZero();

        OutboxMessage first = repository.findByEventId("e-a").orElseThrow();
        assertThat(first.isPublished()).isFalse();
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).contains("broker caído");
        assertThat(repository.countByPublishedAtIsNull()).isEqualTo(2);

        broker.down = false;
        // Durante el backoff el relay no reintenta, aunque el broker ya se haya recuperado.
        assertThat(relay.relayPending()).isZero();
        clock.advance(Duration.ofMinutes(1));
        assertThat(relay.relayPending()).isEqualTo(2);
        assertThat(broker.sent).containsExactly(event("e-a", "q-a"), event("e-b", "q-b"));
        assertThat(repository.findByEventId("e-a").orElseThrow().getLastError()).isNull();
    }

    @Test
    void elBackoffCreceExponencialmenteHastaUnTope() {
        assertThat(relay.backoffFor(1)).isEqualTo(Duration.ofMillis(500));
        assertThat(relay.backoffFor(2)).isEqualTo(Duration.ofSeconds(1));
        assertThat(relay.backoffFor(4)).isEqualTo(Duration.ofSeconds(4));
        assertThat(relay.backoffFor(10)).isEqualTo(Duration.ofSeconds(30));
        assertThat(relay.backoffFor(500)).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void elHealthReportaPendientesYPasaAOutOfServiceSiEnvejecen() {
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);

        useCase.submit("e-h", "q-h");
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
        assertThat(health.health().getDetails()).containsEntry("pending", 1L);

        clock.advance(Duration.ofMinutes(5));
        assertThat(health.health().getStatus()).isEqualTo(Status.OUT_OF_SERVICE);

        relay.relayPending();
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
        assertThat(health.health().getDetails()).containsEntry("pending", 0L);
    }

    @Test
    void sePurganLosMensajesPublicadosAntiguos() {
        useCase.submit("e-old", "q-old");
        relay.relayPending();
        clock.advance(Duration.ofDays(8));
        useCase.submit("e-new", "q-new");

        assertThat(relay.purgePublishedOlderThan(Duration.ofDays(7))).isEqualTo(1);
        assertThat(repository.findByEventId("e-old")).isEmpty();
        assertThat(repository.findByEventId("e-new")).isPresent();
    }
}
