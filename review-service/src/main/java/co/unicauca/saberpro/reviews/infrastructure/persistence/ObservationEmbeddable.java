package co.unicauca.saberpro.reviews.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

@Embeddable
public class ObservationEmbeddable {

    @Column(nullable = false, length = 60)
    private String reviewerLogin;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    protected ObservationEmbeddable() {
    }

    public ObservationEmbeddable(String reviewerLogin, String text, Instant createdAt) {
        this.reviewerLogin = reviewerLogin;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getReviewerLogin() { return reviewerLogin; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }
}
