package co.unicauca.saberpro.review.application.port.in;

import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;

import java.util.List;

/** Puerto de entrada (HU04): el administrador asigna entre 1 y 3 revisores. */
public interface AssignReviewersUseCase {

    /** Datos del caso de uso; el administrador sale del token, no del cuerpo de la peticion. */
    record AssignReviewersCommand(String questionId, List<String> reviewerLogins, String adminLogin) {
    }

    List<ReviewerAssignment> assign(AssignReviewersCommand command);
}
