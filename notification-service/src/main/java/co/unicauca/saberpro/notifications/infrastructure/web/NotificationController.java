package co.unicauca.saberpro.notifications.infrastructure.web;

import co.unicauca.saberpro.notifications.application.NotificationService;
import co.unicauca.saberpro.notifications.domain.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "Bandeja de notificaciones del usuario autenticado")
public class NotificationController {

    static final String USER_HEADER = "X-User-Login";
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    public record NotificationResponse(Long id, String eventType, String questionId, String subject, String body,
                                       String deliveryStatus, boolean read, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getEventType(), n.getQuestionId(), n.getSubject(),
                    n.getBody(), n.getDeliveryStatus().name(), n.isRead(), n.getCreatedAt());
        }
    }

    public record PageResponse(List<NotificationResponse> items, int page, int size, long totalItems,
                               int totalPages, long unread) {
    }

    @GetMapping
    @Operation(summary = "Notificaciones del usuario (más recientes primero)")
    public PageResponse inbox(@RequestHeader(USER_HEADER) String user,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "10") int size) {
        var result = service.inbox(user, page, size).map(NotificationResponse::from);
        return new PageResponse(result.items(), result.page(), result.size(), result.totalItems(),
                result.totalPages(), service.unreadCount(user));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca una notificación como leída")
    public NotificationResponse markRead(@RequestHeader(USER_HEADER) String user, @PathVariable Long id) {
        return NotificationResponse.from(service.markRead(id, user));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> notFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("status", 404, "message", ex.getMessage(), "timestamp", Instant.now().toString()));
    }
}
