package co.unicauca.saberpro.api.dto;

import java.util.List;

/** Formato uniforme de los errores devueltos por la API. */
public record ApiErrorDTO(String timestamp, int status, String error, List<String> messages, String path) {
}
