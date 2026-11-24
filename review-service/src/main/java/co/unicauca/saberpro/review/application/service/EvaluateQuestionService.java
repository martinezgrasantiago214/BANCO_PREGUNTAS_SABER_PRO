package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.review.application.port.in.EvaluateQuestionUseCase;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.domain.exception.ReviewNotFoundException;
import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;

import java.time.Clock;

/**
 * Caso de uso HU05: un revisor evalua la pregunta. Las reglas (asignado,
 * ronda sin veredicto, observaciones al rechazar) las aplica el agregado; el
 * evento QuestionEvaluated hace que el question-service cambie el estado y
 * que el notification-service avise al autor y a los administradores.
 */
public class EvaluateQuestionService implements EvaluateQuestionUseCase {

    private final QuestionReviewRepository repository;
    private final ReviewEventPublisher publisher;
    private final Clock clock;

    public EvaluateQuestionService(QuestionReviewRepository repository, ReviewEventPublisher publisher,
                                   Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Evaluation evaluate(EvaluateQuestionCommand command) {
        String questionId = Texts.require(command.questionId(), "Debe indicar la pregunta a evaluar.");
        QuestionReview review = repository.findById(questionId)
                .orElseThrow(() -> new ReviewNotFoundException(questionId));

        Evaluation evaluation = review.evaluate(command.reviewerLogin(), command.verdict(), command.observations(),
                clock.instant());
        repository.save(review);
        publisher.publish(review.pullDomainEvents());
        return evaluation;
    }
}
