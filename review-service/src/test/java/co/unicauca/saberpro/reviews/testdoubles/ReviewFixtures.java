package co.unicauca.saberpro.reviews.testdoubles;

import co.unicauca.saberpro.reviews.domain.model.CriterionScore;
import co.unicauca.saberpro.reviews.domain.model.QuestionReview;
import co.unicauca.saberpro.reviews.domain.model.ReviewCriterion;

import java.util.Arrays;
import java.util.List;

public final class ReviewFixtures {

    private ReviewFixtures() {
    }

    public static QuestionReview pending(String questionId) {
        return QuestionReview.fromSubmission(questionId, "autor1", "¿Qué estilo favorece la escalabilidad?",
                "Diseño de software", "Arquitectura");
    }

    /** Califica todos los criterios de la rubrica con el mismo puntaje. */
    public static List<CriterionScore> allScores(int score) {
        return Arrays.stream(ReviewCriterion.values()).map(c -> new CriterionScore(c, score, "ok")).toList();
    }
}
