package co.unicauca.saberpro.reviews.infrastructure.web;

import co.unicauca.saberpro.reviews.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.reviews.infrastructure.web.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReviewDomainException.class)
    public ResponseEntity<ApiError> handle(ReviewDomainException ex) {
        HttpStatus status = switch (ex.getKind()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_STATE -> HttpStatus.CONFLICT;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
        };
        return build(status, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handle(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handle(MissingRequestHeaderException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Falta el encabezado " + ex.getHeaderName() + ".", List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handle(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Otro revisor actualizó esta revisión al mismo tiempo; intente de nuevo.",
                List.of());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handle(RestClientException ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de usuarios no está disponible.", List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
    }
}
