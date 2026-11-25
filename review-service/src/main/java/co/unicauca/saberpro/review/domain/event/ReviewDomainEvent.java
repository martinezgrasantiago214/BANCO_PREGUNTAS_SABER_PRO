package co.unicauca.saberpro.review.domain.event;

import java.time.Instant;

/**
 * Evento de dominio: algo relevante que ocurrio dentro del agregado
 * QuestionReview. El agregado los acumula y el caso de uso, despues de
 * guardar, los entrega al puerto de salida ReviewEventPublisher, que los
 * traduce a eventos de integracion de RabbitMQ. El dominio no sabe que
 * existe un broker.
 */
public sealed interface ReviewDomainEvent permits ReviewersAssigned, QuestionEvaluated, ReviewRoundReopened {

    String questionId();

    Instant occurredAt();
}
