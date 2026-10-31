package co.unicauca.saberpro.reviews.domain.model;

/** Calificacion de un criterio (entidad "EvaluacionRevisor" del diagrama). */
public record CriterionScore(ReviewCriterion criterion, int score, String comment) {

    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    public CriterionScore {
        if (criterion == null) {
            throw new IllegalArgumentException("El criterio es obligatorio.");
        }
        if (score < MIN_SCORE || score > MAX_SCORE) {
            throw new IllegalArgumentException("La calificación de \"" + criterion.getLabel()
                    + "\" debe estar entre " + MIN_SCORE + " y " + MAX_SCORE + ".");
        }
        comment = comment == null ? "" : comment.trim();
    }
}
