package co.unicauca.saberpro.api.exception;

import co.unicauca.saberpro.api.dto.ApiErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

/** Traduce las excepciones a respuestas JSON con el codigo HTTP adecuado. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorDTO> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()), req);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorDTO> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessages(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorDTO> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                List.of("El cuerpo de la peticion no es un JSON valido o no tiene el formato esperado."), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                List.of("Valor invalido para el parametro '" + ex.getName() + "'."), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDTO> handleGeneric(Exception ex, HttpServletRequest req) {
        // Errores propios de Spring MVC (ruta inexistente -> 404, metodo HTTP no
        // soportado -> 405, etc.) conservan su codigo HTTP original.
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return build(status, List.of(ex.getMessage()), req);
        }
        ex.printStackTrace();
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                List.of("Error interno del servidor: " + ex.getMessage()), req);
    }

    private ResponseEntity<ApiErrorDTO> build(HttpStatus status, List<String> messages, HttpServletRequest req) {
        ApiErrorDTO body = new ApiErrorDTO(LocalDateTime.now().toString(), status.value(),
                status.getReasonPhrase(), messages, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
