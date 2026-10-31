package co.unicauca.saberpro.client.api.dto;

import java.time.Instant;
import java.util.List;

public record NotificationDto(Long id, String eventType, String questionId, String subject, String body,
                              String deliveryStatus, boolean read, Instant createdAt) {

    public record Page(List<NotificationDto> items, int page, int size, long totalItems, int totalPages,
                       long unread) {
    }
}
