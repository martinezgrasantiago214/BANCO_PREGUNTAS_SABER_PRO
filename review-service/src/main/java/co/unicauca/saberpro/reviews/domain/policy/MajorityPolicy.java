package co.unicauca.saberpro.reviews.domain.policy;

import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.reviews.domain.model.Verdict;

import java.util.List;
import java.util.Optional;

/**
 * Politica alternativa: decide en cuanto un concepto alcanza la mayoria
 * absoluta de los revisores asignados (con 2 revisores en empate gana el
 * rechazo, por prudencia academica).
 */
public class MajorityPolicy implements DecisionPolicy {

    @Override
    public Optional<Verdict> decide(List<ReviewerAssignment> assignments) {
        int total = assignments.size();
        if (total == 0) {
            return Optional.empty();
        }
        long approved = count(assignments, Verdict.APROBADA);
        long rejected = count(assignments, Verdict.RECHAZADA);
        int majority = total / 2 + 1;
        if (approved >= majority) {
            return Optional.of(Verdict.APROBADA);
        }
        if (rejected >= majority || (approved + rejected == total)) {
            return Optional.of(Verdict.RECHAZADA);
        }
        return Optional.empty();
    }

    private static long count(List<ReviewerAssignment> assignments, Verdict verdict) {
        return assignments.stream().filter(a -> a.isCompleted() && a.getVerdict() == verdict).count();
    }

    @Override
    public String name() {
        return "MAJORITY";
    }
}
