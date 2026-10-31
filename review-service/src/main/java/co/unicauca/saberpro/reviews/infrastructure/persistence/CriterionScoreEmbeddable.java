package co.unicauca.saberpro.reviews.infrastructure.persistence;

import co.unicauca.saberpro.reviews.domain.model.ReviewCriterion;
import jakarta.persistence.*;

@Embeddable
public class CriterionScoreEmbeddable {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReviewCriterion criterion;

    @Column(nullable = false)
    private int score;

    @Column(length = 1000)
    private String comment;

    protected CriterionScoreEmbeddable() {
    }

    public CriterionScoreEmbeddable(ReviewCriterion criterion, int score, String comment) {
        this.criterion = criterion;
        this.score = score;
        this.comment = comment;
    }

    public ReviewCriterion getCriterion() { return criterion; }
    public int getScore() { return score; }
    public String getComment() { return comment; }
}
