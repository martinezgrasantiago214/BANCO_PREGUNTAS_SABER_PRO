package co.unicauca.saberpro.users.infrastructure.web;

import co.unicauca.saberpro.users.domain.exception.UserDomainException;
import co.unicauca.saberpro.users.infrastructure.web.dto.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/** Traduce las excepciones de dominio a respuestas HTTP coherentes. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<ApiError> handle(UserDomainException ex) {
        HttpStatus status = switch (ex.getKind()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case DUPLICATED -> HttpStatus.CONFLICT;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
        };
        return build(status, ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handle(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return build(HttpStatus.BAD_REQUEST, "Solicitud inválida.", details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handle(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), List.of());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
    }
}
