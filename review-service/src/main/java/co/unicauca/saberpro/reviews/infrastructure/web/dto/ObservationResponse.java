package co.unicauca.saberpro.reviews.infrastructure.web.dto;

import co.unicauca.saberpro.reviews.domain.model.Observation;

import java.time.Instant;

public record ObservationResponse(String reviewerLogin, String text, Instant createdAt) {

    public static ObservationResponse from(Observation o) {
        return new ObservationResponse(o.reviewerLogin(), o.text(), o.createdAt());
    }
}
