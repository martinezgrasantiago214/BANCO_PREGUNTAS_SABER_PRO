package co.unicauca.saberpro.review.infrastructure.adapter.in.web;

import co.unicauca.saberpro.common.contract.ApiError;
import co.unicauca.saberpro.review.application.port.out.DirectoryUnavailableException;
import co.unicauca.saberpro.review.domain.exception.ConcurrentReviewUpdateException;
import co.unicauca.saberpro.review.domain.exception.ReviewConflictException;
import co.unicauca.saberpro.review.domain.exception.ReviewDomainException;
import co.unicauca.saberpro.review.domain.exception.ReviewNotFoundException;
import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;
import co.unicauca.saberpro.review.domain.exception.ReviewerNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Traduce las excepciones del dominio de revision a respuestas HTTP. El
 * dominio no conoce codigos HTTP; este adaptador decide:
 * regla incumplida 400, no permitido 403, no existe 404, conflicto de estado
 * o de concurrencia 409, directorio caido 503.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class ReviewApiExceptionHandler {

    @ExceptionHandler(ReviewDomainException.class)
    public ResponseEntity<ApiError> handleDomain(ReviewDomainException ex, HttpServletRequest req) {
        return build(statusOf(ex), ex.getMessages(), req);
    }

    @ExceptionHandler(DirectoryUnavailableException.class)
    public ResponseEntity<ApiError> handleDirectory(DirectoryUnavailableException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, List.of(ex.getMessage()), req);
    }

    static HttpStatus statusOf(ReviewDomainException ex) {
        if (ex instanceof ReviewRuleViolationException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof ReviewerNotAllowedException) {
            return HttpStatus.FORBIDDEN;
        }
        if (ex instanceof ReviewNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (ex instanceof ReviewConflictException || ex instanceof ConcurrentReviewUpdateException) {
            return HttpStatus.CONFLICT;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, List<String> messages, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ApiError(LocalDateTime.now().toString(), status.value(),
                status.getReasonPhrase(), messages, req.getRequestURI()));
    }
}
