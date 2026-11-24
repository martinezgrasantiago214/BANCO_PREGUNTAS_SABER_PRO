package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.review.application.port.in.AssignReviewersUseCase;
import co.unicauca.saberpro.review.application.port.out.QuestionReviewRepository;
import co.unicauca.saberpro.review.application.port.out.ReviewEventPublisher;
import co.unicauca.saberpro.review.application.port.out.ReviewerDirectory;
import co.unicauca.saberpro.review.domain.exception.ReviewNotFoundException;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.review.domain.model.ReviewerCandidate;
import co.unicauca.saberpro.review.domain.model.ReviewerSelection;

import java.time.Clock;
import java.util.List;

/**
 * Caso de uso HU04: asignar revisores.
 *
 * 1. Valida la seleccion (1 a 3 logins distintos) antes de consultar a nadie.
 * 2. Carga el agregado y consulta en el directorio el rol de cada candidato.
 * 3. El agregado aplica todas las reglas y registra ReviewersAssigned.
 * 4. Guarda (con bloqueo optimista) y publica el evento: el
 *    notification-service envia el correo a cada revisor.
 */
public class AssignReviewersService implements AssignReviewersUseCase {

    private final QuestionReviewRepository repository;
    private final ReviewerDirectory directory;
    private final ReviewEventPublisher publisher;
    private final Clock clock;

    public AssignReviewersService(QuestionReviewRepository repository, ReviewerDirectory directory,
                                  ReviewEventPublisher publisher, Clock clock) {
        this.repository = repository;
        this.directory = directory;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public List<ReviewerAssignment> assign(AssignReviewersCommand command) {
        String questionId = Texts.require(command.questionId(),
                "Debe indicar la pregunta a la que se asignan los revisores.");
        if (command.adminLogin() == null || command.adminLogin().isBlank()) {
            throw new ReviewRuleViolationException("Debe indicar el administrador que asigna los revisores.");
        }
        ReviewerSelection selection = new ReviewerSelection(command.reviewerLogins());

        QuestionReview review = repository.findById(questionId)
                .orElseThrow(() -> new ReviewNotFoundException(questionId));
        List<ReviewerCandidate> candidates = selection.logins().stream().map(directory::lookup).toList();

        List<ReviewerAssignment> created = review.assignReviewers(candidates, command.adminLogin(), clock.instant());
        repository.save(review);
        publisher.publish(review.pullDomainEvents());
        return created;
    }
}
