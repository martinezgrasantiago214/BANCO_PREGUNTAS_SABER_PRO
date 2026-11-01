package co.unicauca.saberpro.review.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.review.access.IReviewRepository;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;

import java.util.Comparator;
import java.util.List;

/**
 * Casos de uso de la revision por pares. Los marcados con TODO(PSP-xx)
 * responden 501 hasta que se implementen; el dominio (ReviewAssignment,
 * Evaluation, ReviewPolicy), el repositorio y el bus de eventos ya estan
 * listos para usarse.
 */
public class ReviewApplicationService {

    private final IReviewRepository repository;
    private final DomainEventBus eventBus;

    public ReviewApplicationService(IReviewRepository repository, DomainEventBus eventBus) {
        this.repository = repository;
        this.eventBus = eventBus;
    }

    /**
     * Evento del question-service: una pregunta quedo Pendiente de revision.
     *
     * TODO(PSP-30): guardar new PendingQuestion(event.questionId(), ...) con repository.savePendingQuestion.
     */
    public void registerPendingQuestion(QuestionSubmittedForReviewEvent event) {
        throw new PendingIssueException("PSP-30", "registrar preguntas pendientes de revision (HU04)");
    }

    /** TODO(PSP-30): HU04 - devolver repository.findPendingQuestions() para que el administrador las vea. */
    public List<PendingQuestion> listPendingQuestions() {
        throw new PendingIssueException("PSP-30", "listar preguntas pendientes de revision (HU04)");
    }

    /**
     * HU04: el administrador asigna entre 1 y 3 revisores.
     *
     * TODO(PSP-31, PSP-32, PSP-33, PSP-34): validar con ReviewPolicy.validateReviewers (y que la pregunta
     * este pendiente), guardar un ReviewAssignment por revisor y publicar ReviewersAssignedEvent en
     * eventBus para que el notification-service envie los correos.
     */
    public List<ReviewAssignment> assignReviewers(String questionId, List<String> reviewerLogins, String adminLogin) {
        throw new PendingIssueException("PSP-32", "asignar revisores a una pregunta (HU04)");
    }

    /**
     * HU05 (PSP-45): preguntas que el administrador le asigno a un revisor,
     * de la asignacion mas reciente a la mas antigua. Solo devuelve las de
     * ese revisor.
     */
    public List<ReviewAssignment> listAssignedTo(String reviewerLogin) {
        String reviewer = requireText(reviewerLogin, "Debe indicar el login del revisor.");
        return repository.findAssignmentsByReviewer(reviewer).stream()
                .sorted(Comparator.comparing(ReviewAssignment::getAssignedAt).reversed())
                .toList();
    }

    /**
     * HU05: el revisor deja su evaluacion y observaciones.
     *
     * TODO(PSP-47, PSP-48, PSP-49): verificar que la pregunta este asignada a ese revisor y pendiente,
     * guardar la Evaluation y publicar QuestionEvaluatedEvent en eventBus (el question-service cambia
     * el estado y el notification-service avisa al autor y al administrador).
     */
    public Evaluation evaluate(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                               String observations) {
        throw new PendingIssueException("PSP-47", "registrar la evaluacion y observaciones (HU05)");
    }

    /** TODO(PSP-47): HU05 - el autor lee las observaciones (repository.findEvaluationsByQuestion). */
    public List<Evaluation> listEvaluations(String questionId) {
        throw new PendingIssueException("PSP-47", "consultar las observaciones de una pregunta (HU05)");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
