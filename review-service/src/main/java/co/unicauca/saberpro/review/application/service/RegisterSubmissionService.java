package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.review.application.port.in.RegisterSubmissionUseCase;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.QuestionSubmission;

import java.util.Optional;

/**
 * Caso de uso: registrar que una version de la pregunta llego a revision.
 * Si es la primera vez abre la revision; si es una version mejorada (HU06)
 * el agregado decide si abre una ronda nueva. Solo orquesta: cargar,
 * delegar en el agregado, guardar y publicar.
 */
public class RegisterSubmissionService implements RegisterSubmissionUseCase {

    private final QuestionReviewRepository repository;
    private final ReviewEventPublisher publisher;

    public RegisterSubmissionService(QuestionReviewRepository repository, ReviewEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public boolean register(QuestionSubmission submission) {
        Optional<QuestionReview> existing = repository.findById(submission.questionId());
        QuestionReview review;
        boolean changed;
        if (existing.isEmpty()) {
            review = QuestionReview.open(submission);
            changed = true;
        } else {
            review = existing.get();
            changed = review.receive(submission);
        }
        if (changed) {
            repository.save(review);
            publisher.publish(review.pullDomainEvents());
        }
        return changed;
    }
}
