package co.unicauca.saberpro.review.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Objeto de valor (HU04): un revisor que el administrador asigno en una ronda de revision. */
public record ReviewerAssignment(String reviewerLogin, String assignedBy, Instant assignedAt) {

    public ReviewerAssignment {
        Objects.requireNonNull(reviewerLogin, "reviewerLogin");
        Objects.requireNonNull(assignedAt, "assignedAt");
    }

    public boolean isFor(String login) {
        return reviewerLogin.equals(login);
    }
}
