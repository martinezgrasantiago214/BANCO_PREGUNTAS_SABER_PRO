package co.unicauca.saberpro.reviews.domain.policy;

import co.unicauca.saberpro.reviews.domain.model.ReviewerAssignment;
import co.unicauca.saberpro.reviews.domain.model.Verdict;

import java.util.List;
import java.util.Optional;

/**
 * Patron Strategy (GoF, comportamiento): regla con la que se combina el
 * concepto de varios revisores (1 a 3) en una decision final. El comite
 * academico puede cambiar la politica por configuracion sin tocar el
 * agregado ni el servicio (OCP).
 *
 * @return la decision final, o vacio si todavia faltan conceptos.
 */
public interface DecisionPolicy {

    Optional<Verdict> decide(List<ReviewerAssignment> assignments);

    String name();
}
