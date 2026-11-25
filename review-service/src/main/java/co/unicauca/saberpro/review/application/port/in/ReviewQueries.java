package co.unicauca.saberpro.review.application.port.in;

import co.unicauca.saberpro.review.domain.model.Evaluation;
import co.unicauca.saberpro.review.domain.model.QuestionReview;
import co.unicauca.saberpro.review.domain.model.ReviewStatus;
import co.unicauca.saberpro.review.domain.model.ReviewerAssignment;

import java.util.List;

/** Puerto de entrada de consultas (lado de lectura): HU04, HU05 y HU06. */
public interface ReviewQueries {

    /** Una asignacion de un revisor con los datos de su ronda (HU05). */
    record AssignedReview(String questionId, int round, int questionVersion, String directQuestion,
                          ReviewerAssignment assignment, ReviewStatus roundStatus, boolean currentRound) {

        /** El revisor todavia puede evaluarla: es la ronda vigente y no tiene veredicto. */
        public boolean awaitingMyEvaluation() {
            return currentRound && roundStatus.acceptsEvaluation();
        }
    }

    /** HU04: preguntas que esperan veredicto, de la enviada hace mas tiempo a la mas reciente. */
    List<QuestionReview> pendingQuestions();

    /** HU05: asignaciones de un revisor, de la mas reciente a la mas antigua. */
    List<AssignedReview> assignedTo(String reviewerLogin);

    /**
     * HU06: historial completo de revisiones de una pregunta. Solo lo ven el
     * autor, los administradores y quienes hayan sido sus revisores.
     */
    QuestionReview history(String questionId, String requesterLogin, boolean requesterIsAdmin);

    /** HU05/HU06: todas las evaluaciones de la pregunta, en orden (mismas reglas de visibilidad). */
    List<Evaluation> evaluations(String questionId, String requesterLogin, boolean requesterIsAdmin);
}
