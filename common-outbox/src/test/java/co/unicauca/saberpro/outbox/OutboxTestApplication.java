package co.unicauca.saberpro.outbox;

import co.unicauca.saberpro.events.QuestionSubmittedForReviewEvent;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.TransactionManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Aplicacion minima para probar la auto-configuracion con H2 y un broker falso. */
@SpringBootApplication
public class OutboxTestApplication {

    @Bean
    RecordingSender recordingSender() {
        return new RecordingSender();
    }

    @Bean
    MutableClock outboxClock() {
        return new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
    }

    @Bean
    FakeUseCase fakeUseCase(OutboxEventWriter writer) {
        return new FakeUseCase(writer);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor useCaseTransactions(TransactionManager tm) {
        return UseCaseTransactions.advisor(tm, FakeUseCase.class);
    }

    static QuestionSubmittedForReviewEvent event(String eventId, String questionId) {
        return new QuestionSubmittedForReviewEvent(eventId, Instant.parse("2026-10-31T12:00:00Z"), questionId,
                "autor1", "¿Qué estilo favorece la escalabilidad?", "Diseño", "Arquitectura", "Estilos", "MEDIO");
    }

    /** Servicio de aplicacion "puro" (sin anotaciones de Spring), como QuestionService. */
    public static class FakeUseCase {

        private final OutboxEventWriter writer;

        public FakeUseCase(OutboxEventWriter writer) {
            this.writer = writer;
        }

        public void submit(String eventId, String questionId) {
            writer.enqueue("question.submitted", event(eventId, questionId));
        }

        public void submitThenFail(String eventId, String questionId) {
            writer.enqueue("question.submitted", event(eventId, questionId));
            throw new IllegalStateException("fallo despues de escribir en la outbox");
        }
    }

    /** Broker falso: registra lo enviado y puede simular una caida. */
    public static class RecordingSender implements OutboxMessageSender {

        final List<Object> sent = new ArrayList<>();
        final List<String> routingKeys = new ArrayList<>();
        boolean down;

        @Override
        public void send(String routingKey, Object event) {
            if (down) {
                throw new IllegalStateException("broker caído");
            }
            routingKeys.add(routingKey);
            sent.add(event);
        }
    }

    public static class MutableClock extends java.time.Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(java.time.Duration d) {
            now = now.plus(d);
        }

        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
