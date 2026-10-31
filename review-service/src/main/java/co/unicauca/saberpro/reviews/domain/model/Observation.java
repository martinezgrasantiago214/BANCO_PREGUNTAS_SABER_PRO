package co.unicauca.saberpro.reviews.domain.model;

import java.time.Instant;

/** Observacion que un revisor deja para que la lea el autor (HU05). */
public record Observation(String reviewerLogin, String text, Instant createdAt) {

    public static final int MAX_LENGTH = 2000;

    public Observation {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("La observación no puede estar vacía.");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("La observación no puede superar " + MAX_LENGTH + " caracteres.");
        }
        text = text.trim();
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
