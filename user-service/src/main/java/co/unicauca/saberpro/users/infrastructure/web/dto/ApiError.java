package co.unicauca.saberpro.users.infrastructure.web.dto;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String message, List<String> details) {
}
