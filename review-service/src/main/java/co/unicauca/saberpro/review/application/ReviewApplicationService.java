package co.unicauca.saberpro.review.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.common.events.DomainEventBus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.events.QuestionSubmittedForReviewEvent;
import co.unicauca.saberpro.common.events.ReviewersAssignedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.review.access.IReviewRepository;
import co.unicauca.saberpro.review.directory.UserRoleDirectory;
import co.unicauca.saberpro.review.domain.Evaluation;
import co.unicauca.saberpro.review.domain.EvaluationPolicy;
import co.unicauca.saberpro.review.domain.PendingQuestion;
import co.unicauca.saberpro.review.domain.ReviewAssignment;
import co.unicauca.saberpro.review.domain.ReviewPolicy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Casos de uso de la revision por pares: asignacion de revisores (HU04) y
 * evaluacion de preguntas (HU05).
 */
public class ReviewApplicationService {

    private final IReviewRepository repository;
    private final DomainEventBus eventBus;
    private final UserRoleDirectory reviewerRoles;

    public ReviewApplicationService(IReviewRepository repository, DomainEventBus eventBus,
                                    UserRoleDirectory reviewerRoles) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.reviewerRoles = reviewerRoles;
    }

    /**
     * HU04 (PSP-30): evento del question-service, una pregunta quedo Pendiente
     * de revision. Se guarda una copia local para que el administrador la vea.
     * Si el mismo evento llega dos veces, la copia simplemente se reemplaza.
     */
    public void registerPendingQuestion(QuestionSubmittedForReviewEvent event) {
        if (event == null || event.questionId() == null || event.questionId().isBlank()) {
            throw new BusinessException("El evento no trae el identificador de la pregunta.");
        }
        Instant submittedAt = event.occurredAt() != null ? event.occurredAt() : Instant.now();
        repository.savePendingQuestion(new PendingQuestion(event.questionId().trim(), event.authorLogin(),
                event.directQuestion(), submittedAt));
    }

    /**
     * HU04 (PSP-30): preguntas Pendientes de revision que el administrador
     * puede asignar, de la mas antigua a la mas reciente. Las que ya tienen
     * un veredicto (HU05) dejaron de estar pendientes y no se listan.
     */
    public List<PendingQuestion> listPendingQuestions() {
        return repository.findPendingQuestions().stream()
                .filter(q -> repository.findEvaluationsByQuestion(q.questionId()).isEmpty())
                .sorted(Comparator.comparing(PendingQuestion::submittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** HU04: revisores ya asignados a una pregunta, en el orden en que se asignaron. */
    public List<ReviewAssignment> listAssignmentsOf(String questionId) {
        String question = requireText(questionId, "Debe indicar la pregunta.");
        return repository.findAssignmentsByQuestion(question).stream()
                .sorted(Comparator.comparing(ReviewAssignment::getAssignedAt))
                .toList();
    }

    /**
     * HU04: el administrador asigna entre 1 y 3 revisores a una pregunta.
     *
     * - PSP-31: solo usuarios con rol REVISOR, nunca el autor de la pregunta,
     *   y entre 1 y 3 revisores distintos contando los que ya tenia asignados.
     * - PSP-32: solo preguntas Pendientes de revision; se publica
     *   ReviewersAssignedEvent para que el notification-service envie los correos (PSP-34).
     * - PSP-33: cada asignacion queda guardada en el repositorio; un revisor no
     *   se asigna dos veces a la misma pregunta.
     *
     * Es synchronized para que dos asignaciones simultaneas a la misma
     * pregunta no superen entre las dos el maximo de revisores.
     *
     * @return las asignaciones nuevas.
     */
    public synchronized List<ReviewAssignment> assignReviewers(String questionId, List<String> reviewerLogins,
                                                               String adminLogin) {
        String question = requireText(questionId, "Debe indicar la pregunta a la que se asignan los revisores.");
        String admin = requireText(adminLogin, "Debe indicar el administrador que asigna los revisores.");
        List<String> reviewers = reviewerLogins == null ? List.of()
                : reviewerLogins.stream().map(login -> login == null ? null : login.trim()).toList();

        List<String> errors = new ArrayList<>(ReviewPolicy.validateReviewers(reviewers));
        if (!errors.isEmpty()) {
            throw new BusinessException(errors);
        }

        PendingQuestion pending = repository.findPendingQuestion(question).orElseThrow(() ->
                new BusinessException("La pregunta " + question + " no esta Pendiente de revision."));
        if (!repository.findEvaluationsByQuestion(question).isEmpty()) {
            throw new BusinessException("La pregunta " + question
                    + " ya fue evaluada y no esta Pendiente de revision.");
        }

        List<String> alreadyAssigned = repository.findAssignmentsByQuestion(question).stream()
                .map(ReviewAssignment::getReviewerLogin).toList();
        for (String reviewer : reviewers) {
            if (alreadyAssigned.contains(reviewer)) {
                errors.add("El revisor " + reviewer + " ya esta asignado a esta pregunta.");
            } else if (reviewer.equals(pending.authorLogin())) {
                errors.add("No puede asignar al autor (" + reviewer + ") como revisor de su propia pregunta.");
            } else {
                Optional<Role> role = reviewerRoles.findRole(reviewer);
                if (role.isEmpty()) {
                    errors.add("El usuario " + reviewer + " no existe.");
                } else if (role.get() != Role.REVISOR) {
                    errors.add("El usuario " + reviewer + " no tiene el rol Revisor.");
                }
            }
        }
        if (alreadyAssigned.size() + reviewers.size() > ReviewPolicy.MAX_REVIEWERS) {
            errors.add("La pregunta ya tiene " + alreadyAssigned.size() + " revisor(es) asignado(s); "
                    + "no puede tener mas de " + ReviewPolicy.MAX_REVIEWERS + ".");
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(errors);
        }

        Instant now = Instant.now();
        List<ReviewAssignment> created = new ArrayList<>();
        for (String reviewer : reviewers) {
            ReviewAssignment assignment = new ReviewAssignment(question, reviewer, admin, now);
            repository.saveAssignment(assignment);
            created.add(assignment);
        }

        eventBus.publish(new ReviewersAssignedEvent(question, pending.directQuestion(), reviewers, admin, now));
        return created;
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
     * HU05 (PSP-47): el revisor registra su veredicto y sus observaciones.
     *
     * Solo se evalua una pregunta que este asignada a ese revisor y Pendiente
     * de revision. Regla de decision: el primer revisor asignado que evalua
     * decide el estado (Aprobada o Rechazada). Desde ese momento la pregunta
     * ya no esta pendiente, asi que no admite mas evaluaciones, ni de ese
     * revisor ni de los demas asignados.
     *
     * Es synchronized para que dos evaluaciones simultaneas de la misma
     * pregunta no se registren las dos.
     */
    public synchronized Evaluation evaluate(String questionId, String reviewerLogin, EvaluationVerdict verdict,
                                            String observations) {
        String question = requireText(questionId, "Debe indicar la pregunta a evaluar.");
        String reviewer = requireText(reviewerLogin, "Debe indicar el login del revisor.");
        List<String> errors = EvaluationPolicy.validate(verdict, observations);
        if (!errors.isEmpty()) {
            throw new BusinessException(errors);
        }

        boolean assigned = repository.findAssignmentsByQuestion(question).stream()
                .anyMatch(a -> a.isFor(question, reviewer));
        if (!assigned) {
            throw new BusinessException("La pregunta " + question + " no esta asignada al revisor " + reviewer + ".");
        }
        PendingQuestion pending = repository.findPendingQuestion(question).orElseThrow(() ->
                new BusinessException("La pregunta " + question + " no esta Pendiente de revision."));
        List<Evaluation> previous = repository.findEvaluationsByQuestion(question);
        if (!previous.isEmpty()) {
            Evaluation first = previous.get(0);
            throw new BusinessException("La pregunta ya fue evaluada por " + first.getReviewerLogin() + " ("
                    + first.getVerdict().getLabel() + ") y ya no esta Pendiente de revision.");
        }

        Evaluation evaluation = new Evaluation(question, reviewer, verdict, observations);
        repository.saveEvaluation(evaluation);

        // PSP-49: el question-service cambia el estado y el notification-service avisa al autor
        // y a los administradores.
        eventBus.publish(new QuestionEvaluatedEvent(question, pending.authorLogin(), pending.directQuestion(),
                reviewer, verdict, evaluation.getObservations(), evaluation.getEvaluatedAt()));
        return evaluation;
    }

    /** HU05 (PSP-47): evaluaciones y observaciones de una pregunta, para que las lea el autor. */
    public List<Evaluation> listEvaluations(String questionId) {
        String question = requireText(questionId, "Debe indicar la pregunta.");
        return repository.findEvaluationsByQuestion(question).stream()
                .sorted(Comparator.comparing(Evaluation::getEvaluatedAt))
                .toList();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }
}
