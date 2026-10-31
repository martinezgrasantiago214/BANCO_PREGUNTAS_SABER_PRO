package co.unicauca.saberpro.reviews.application;

import co.unicauca.saberpro.reviews.application.port.ReviewEventPublisherPort;
import co.unicauca.saberpro.reviews.application.port.ReviewRepositoryPort;
import co.unicauca.saberpro.reviews.application.port.UserDirectoryPort;
import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException.Kind;
import co.unicauca.saberpro.reviews.domain.model.AssignmentStatus;
import co.unicauca.saberpro.reviews.domain.model.Observation;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.reviews.domain.model.Verdict;
import co.unicauca.saberpro.reviews.domain.policy.DecisionPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Casos de uso de Revision por Pares (HU04 y HU05). */
public class ReviewService {

    static final String ROLE_ADMIN = "ADMINISTRADOR";
    static final String ROLE_REVIEWER = "REVISOR";

    private final ReviewRepositoryPort repository;
    private final ReviewEventPublisherPort publisher;
    private final UserDirectoryPort userDirectory;
    private final DecisionPolicy decisionPolicy;

    public ReviewService(ReviewRepositoryPort repository, ReviewEventPublisherPort publisher,
                         UserDirectoryPort userDirectory, DecisionPolicy decisionPolicy) {
        this.repository = repository;
        this.publisher = publisher;
        this.userDirectory = userDirectory;
        this.decisionPolicy = decisionPolicy;
    }

    /** Reaccion a QuestionSubmittedForReviewEvent. Idempotente. */
    public QuestionReview registerSubmission(String questionId, String authorLogin, String statement,
                                             String competence, String topic) {
        Optional<QuestionReview> existing = repository.findByQuestionId(questionId);
        if (existing.isEmpty()) {
            return repository.save(QuestionReview.fromSubmission(questionId, authorLogin, statement, competence, topic));
        }
        QuestionReview review = existing.get();
        return review.resubmit(statement, competence, topic) ? repository.save(review) : review;
    }

    /** HU04: preguntas que esperan revisores. */
    public PageResult<QuestionReview> listPendingAssignment(int page, int size) {
        return repository.findPendingAssignment(normalizePage(page), normalizeSize(size));
    }

    /** HU04: asigna de 1 a 3 revisores y dispara la notificacion por evento. */
    public QuestionReview assignReviewers(String questionId, List<String> reviewerLogins, String adminLogin) {
        if (!userDirectory.hasActiveRole(adminLogin, ROLE_ADMIN)) {
            throw new ReviewDomainException(Kind.FORBIDDEN, "Solo un administrador puede asignar revisores.");
        }
        QuestionReview review = get(questionId);
        // Se valida contra user-service ANTES de modificar el agregado.
        List<String> invalid = new ArrayList<>();
        if (reviewerLogins != null) {
            reviewerLogins.stream().filter(l -> l != null && !l.isBlank()).map(String::trim).distinct()
                    .filter(login -> !userDirectory.hasActiveRole(login, ROLE_REVIEWER))
                    .forEach(invalid::add);
        }
        if (!invalid.isEmpty()) {
            throw new ReviewDomainException(Kind.VALIDATION,
                    "Los siguientes usuarios no son revisores activos: " + String.join(", ", invalid), invalid);
        }
        review.assignReviewers(reviewerLogins, adminLogin);
        QuestionReview saved = repository.save(review);
        publisher.reviewersAssigned(saved);
        return saved;
    }

    /** HU05: preguntas asignadas al revisor (pendientes, completadas o todas). */
    public PageResult<QuestionReview> listAssignedTo(String reviewerLogin, AssignmentStatus status, int page, int size) {
        return repository.findAssignedTo(reviewerLogin, status, normalizePage(page), normalizeSize(size));
    }

    public QuestionReview get(String questionId) {
        return repository.findByQuestionId(questionId)
                .orElseThrow(() -> new ReviewDomainException(Kind.NOT_FOUND,
                        "No hay un proceso de revisión para la pregunta " + questionId + "."));
    }

    /** HU05: evaluacion + concepto Aprobada/Rechazada + observacion opcional. */
    public QuestionReview submitEvaluation(String questionId, String reviewerLogin, EvaluationCommand command) {
        QuestionReview review = get(questionId);
        int observationsBefore = review.getObservations().size();
        Optional<Verdict> decision = review.submitEvaluation(reviewerLogin, command.verdict(), command.scores(),
                command.observation(), decisionPolicy);
        QuestionReview saved = repository.save(review);

        ReviewerAssignment assignment = saved.getAssignments().stream()
                .filter(a -> a.getReviewerLogin().equals(reviewerLogin)).findFirst().orElseThrow();
        publisher.evaluationSubmitted(saved, assignment);
        if (saved.getObservations().size() > observationsBefore) {
            publisher.observationAdded(saved, saved.getObservations().get(saved.getObservations().size() - 1));
        }
        decision.ifPresent(d -> publisher.reviewCompleted(saved));
        return saved;
    }

    /** HU05: observacion suelta para el autor. */
    public Observation addObservation(String questionId, String reviewerLogin, String text) {
        QuestionReview review = get(questionId);
        Observation observation = review.addObservation(reviewerLogin, text);
        QuestionReview saved = repository.save(review);
        publisher.observationAdded(saved, observation);
        return observation;
    }

    /** El autor lee las observaciones; los revisores asignados tambien. */
    public List<Observation> observationsFor(String questionId, String requesterLogin) {
        QuestionReview review = get(questionId);
        boolean allowed = review.getAuthorLogin().equals(requesterLogin)
                || review.isAssignedTo(requesterLogin)
                || userDirectory.hasActiveRole(requesterLogin, ROLE_ADMIN);
        if (!allowed) {
            throw new ReviewDomainException(Kind.FORBIDDEN, "No tiene permiso para ver estas observaciones.");
        }
        return review.getObservations();
    }

    public DecisionPolicy getDecisionPolicy() {
        return decisionPolicy;
    }

    private static int normalizePage(int page) {
        return Math.max(1, page);
    }

    private static int normalizeSize(int size) {
        return Math.min(50, Math.max(1, size));
    }
}
