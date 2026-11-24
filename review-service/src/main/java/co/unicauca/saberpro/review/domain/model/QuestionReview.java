package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.review.domain.event.QuestionEvaluated;
import co.unicauca.saberpro.review.domain.event.ReviewDomainEvent;
import co.unicauca.saberpro.review.domain.event.ReviewRoundReopened;
import co.unicauca.saberpro.review.domain.event.ReviewersAssigned;
import co.unicauca.saberpro.review.domain.exception.ReviewConflictException;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import co.unicauca.saberpro.review.domain.exception.ReviewerNotAllowedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Raiz de agregado (DDD) del contexto "Revision por pares": toda la revision
 * de una pregunta, con sus rondas (una por version, HU06).
 *
 * Segundo corte -> tercer corte: antes las reglas estaban en el servicio de
 * aplicacion y las entidades solo guardaban datos (modelo anemico); el
 * estado "pendiente" se deducia de si habia evaluaciones guardadas. Ahora el
 * estado vive aqui y solo cambia con metodos que expresan el negocio:
 *
 * <ul>
 *   <li>{@link #open} / {@link #receive}: llega una version a revision (HU02, HU06).</li>
 *   <li>{@link #assignReviewers}: el administrador asigna de 1 a 3 revisores (HU04).</li>
 *   <li>{@link #evaluate}: el primer revisor asignado que evalua decide la ronda (HU05).</li>
 * </ul>
 *
 * Invariantes que protege: maximo 3 revisores por ronda, nunca el autor,
 * solo usuarios con rol Revisor, un solo veredicto por ronda, rechazo con
 * observaciones, y una ronda nueva solo despues de un rechazo.
 *
 * Cada metodo registra sus eventos de dominio; el caso de uso los publica
 * despues de guardar ({@link #pullDomainEvents()}). No depende de Spring,
 * ni de la base de datos, ni del broker.
 */
public class QuestionReview {

    private final String questionId;
    private final String authorLogin;
    private final List<ReviewRound> rounds;
    /** Token de concurrencia optimista: lo usa el repositorio para detectar escrituras simultaneas. */
    private final long version;
    private final List<ReviewDomainEvent> domainEvents = new ArrayList<>();

    private QuestionReview(String questionId, String authorLogin, List<ReviewRound> rounds, long version) {
        this.questionId = Objects.requireNonNull(questionId, "questionId");
        this.authorLogin = authorLogin;
        if (rounds.isEmpty()) {
            throw new IllegalArgumentException("Una revision tiene al menos una ronda.");
        }
        this.rounds = new ArrayList<>(rounds);
        this.rounds.sort(Comparator.comparingInt(ReviewRound::number));
        this.version = version;
    }

    /** HU02 -> HU04: la primera version de la pregunta llega a revision. */
    public static QuestionReview open(QuestionSubmission submission) {
        return new QuestionReview(submission.questionId(), submission.authorLogin(),
                List.of(ReviewRound.open(1, submission)), 0);
    }

    /** Reconstruye una revision guardada (adaptador de persistencia). */
    public static QuestionReview restore(String questionId, String authorLogin, List<ReviewRound> rounds,
                                         long version) {
        return new QuestionReview(questionId, authorLogin, rounds, version);
    }

    // ------------------------------------------------------------ comportamiento

    /**
     * Llega a revision una version de la pregunta.
     *
     * <ul>
     *   <li>Si es una version que ya se recibio (RabbitMQ puede entregar el
     *       mismo evento dos veces) no hace nada: es idempotente.</li>
     *   <li>HU06: si es una version mejorada y la ronda actual fue rechazada,
     *       abre una ronda nueva y registra {@link ReviewRoundReopened}.</li>
     *   <li>Si la ronda actual sigue abierta o fue aprobada, la version nueva
     *       no tiene sentido: conflicto.</li>
     * </ul>
     *
     * @return true si se abrio una ronda nueva.
     */
    public boolean receive(QuestionSubmission submission) {
        if (!questionId.equals(submission.questionId())) {
            throw new IllegalArgumentException("El envio es de otra pregunta: " + submission.questionId());
        }
        ReviewRound current = currentRound();
        if (submission.questionVersion() <= current.questionVersion()) {
            return false;
        }
        if (!current.status().allowsNewVersion()) {
            throw new ReviewConflictException("No se puede abrir una revision de la version "
                    + submission.questionVersion() + ": la ronda " + current.number() + " esta "
                    + current.status().getLabel() + ".");
        }
        ReviewRound next = ReviewRound.open(current.number() + 1, submission);
        rounds.add(next);
        domainEvents.add(new ReviewRoundReopened(questionId, submission.questionVersion(), next.number(),
                authorLogin, submission.directQuestion(), current.reviewerLogins(),
                current.evaluation().map(Evaluation::observations).orElse(""), submission.submittedAt()));
        return true;
    }

    /**
     * HU04: el administrador asigna revisores a la ronda actual.
     *
     * Reglas (todas se verifican y se informan juntas): de 1 a 3 revisores
     * distintos sumando los que ya tenia la ronda; solo usuarios existentes
     * con rol Revisor; nunca el autor; nadie dos veces; y solo mientras la
     * ronda no tenga veredicto (si no, 409).
     *
     * @return las asignaciones nuevas.
     */
    public List<ReviewerAssignment> assignReviewers(List<ReviewerCandidate> candidates, String adminLogin,
                                                    Instant now) {
        if (adminLogin == null || adminLogin.isBlank()) {
            throw new ReviewRuleViolationException("Debe indicar el administrador que asigna los revisores.");
        }
        List<ReviewerCandidate> list = candidates == null ? List.of() : candidates;
        new ReviewerSelection(list.stream().map(ReviewerCandidate::login).toList());

        ReviewRound round = currentRound();
        if (!round.status().acceptsAssignments()) {
            throw new ReviewConflictException("La pregunta ya fue evaluada (" + round.status().getLabel()
                    + ") en la ronda " + round.number() + " y no admite mas revisores.");
        }

        List<String> errors = new ArrayList<>();
        for (ReviewerCandidate candidate : list) {
            String login = candidate.login();
            if (round.isAssigned(login)) {
                errors.add("El revisor " + login + " ya esta asignado a esta pregunta.");
            } else if (login.equals(authorLogin)) {
                errors.add("No puede asignar al autor (" + login + ") como revisor de su propia pregunta.");
            } else if (!candidate.exists()) {
                errors.add("El usuario " + login + " no existe.");
            } else if (!candidate.isReviewer()) {
                errors.add("El usuario " + login + " no tiene el rol Revisor.");
            }
        }
        if (list.size() > round.remainingReviewerSlots()) {
            errors.add("La pregunta ya tiene " + round.assignments().size() + " revisor(es) asignado(s); "
                    + "no puede tener mas de " + ReviewerSelection.MAX_REVIEWERS + ".");
        }
        if (!errors.isEmpty()) {
            throw new ReviewRuleViolationException(errors);
        }

        String admin = adminLogin.trim();
        List<ReviewerAssignment> created = list.stream()
                .map(c -> new ReviewerAssignment(c.login(), admin, now))
                .toList();
        round.assign(created);
        domainEvents.add(new ReviewersAssigned(questionId, round.number(), round.directQuestion(),
                created.stream().map(ReviewerAssignment::reviewerLogin).toList(), admin, now));
        return created;
    }

    /**
     * HU05: un revisor deja su veredicto en la ronda actual.
     *
     * Regla de decision: el primer revisor asignado que evalua decide la
     * ronda (Aprobada o Rechazada); despues la ronda no admite mas
     * evaluaciones, ni de el ni de los demas asignados.
     */
    public Evaluation evaluate(String reviewerLogin, EvaluationVerdict verdict, String observations,
                               Instant now) {
        if (reviewerLogin == null || reviewerLogin.isBlank()) {
            throw new ReviewRuleViolationException("Debe indicar el login del revisor.");
        }
        String reviewer = reviewerLogin.trim();
        List<String> contentErrors = Evaluation.validate(verdict, observations);
        if (!contentErrors.isEmpty()) {
            throw new ReviewRuleViolationException(contentErrors);
        }

        ReviewRound round = currentRound();
        if (!round.isAssigned(reviewer)) {
            boolean earlier = rounds.stream().anyMatch(r -> r != round && r.isAssigned(reviewer));
            throw new ReviewerNotAllowedException(earlier
                    ? "La pregunta " + questionId + " esta en la ronda " + round.number() + " (version "
                      + round.questionVersion() + ") y usted no es revisor de esa ronda."
                    : "La pregunta " + questionId + " no esta asignada al revisor " + reviewer + ".");
        }
        if (round.status().isDecided()) {
            Evaluation decisive = round.evaluation().orElseThrow();
            throw new ReviewConflictException("La pregunta ya fue evaluada por " + decisive.reviewerLogin() + " ("
                    + decisive.verdict().getLabel() + ") y ya no esta Pendiente de revision.");
        }

        Evaluation evaluation = Evaluation.issue(reviewer, verdict, observations, now);
        round.decide(evaluation);
        domainEvents.add(new QuestionEvaluated(questionId, round.questionVersion(), round.number(), authorLogin,
                round.directQuestion(), reviewer, verdict, evaluation.observations(), now));
        return evaluation;
    }

    // ------------------------------------------------------------ consultas

    public String questionId() {
        return questionId;
    }

    public String authorLogin() {
        return authorLogin;
    }

    public long version() {
        return version;
    }

    /** Historial de revisiones (HU06): todas las rondas, de la primera a la actual. */
    public List<ReviewRound> rounds() {
        return Collections.unmodifiableList(rounds);
    }

    public ReviewRound currentRound() {
        return rounds.get(rounds.size() - 1);
    }

    public ReviewStatus status() {
        return currentRound().status();
    }

    /** HU04: la pregunta sigue esperando veredicto, asi que el administrador la ve y puede asignarla. */
    public boolean isAwaitingDecision() {
        return status().acceptsAssignments();
    }

    /** Todas las evaluaciones del historial, en orden. */
    public List<Evaluation> evaluations() {
        return rounds.stream().flatMap(r -> r.evaluation().stream()).toList();
    }

    /** Quien puede ver el historial: el autor, los administradores y quien haya sido revisor. */
    public boolean canBeConsultedBy(String login, boolean administrator) {
        return administrator || (login != null && (login.equals(authorLogin)
                || rounds.stream().anyMatch(r -> r.isAssigned(login))));
    }

    /** Entrega (y olvida) los eventos de dominio registrados desde la ultima vez. */
    public List<ReviewDomainEvent> pullDomainEvents() {
        List<ReviewDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
