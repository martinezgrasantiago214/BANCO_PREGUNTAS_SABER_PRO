package co.unicauca.saberpro.client.api.dto;

import java.time.Instant;

public record ObservationDto(String reviewerLogin, String text, Instant createdAt) {
}
