package co.unicauca.saberpro.reviews.domain.policy;

import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.reviews.domain.model.Verdict;

import java.util.List;
import java.util.Optional;

/**
 * Politica por defecto: un solo rechazo rechaza la pregunta de inmediato;
 * se aprueba cuando TODOS los revisores la aprueban.
 */
public class UnanimousApprovalPolicy implements DecisionPolicy {

    @Override
    public Optional<Verdict> decide(List<ReviewerAssignment> assignments) {
        boolean anyRejected = assignments.stream()
                .anyMatch(a -> a.isCompleted() && a.getVerdict() == Verdict.RECHAZADA);
        if (anyRejected) {
            return Optional.of(Verdict.RECHAZADA);
        }
        boolean allApproved = !assignments.isEmpty() && assignments.stream()
                .allMatch(a -> a.isCompleted() && a.getVerdict() == Verdict.APROBADA);
        return allApproved ? Optional.of(Verdict.APROBADA) : Optional.empty();
    }

    @Override
    public String name() {
        return "UNANIMOUS";
    }
}
