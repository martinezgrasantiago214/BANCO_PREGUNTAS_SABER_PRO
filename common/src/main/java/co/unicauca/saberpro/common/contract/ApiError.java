package co.unicauca.saberpro.common.contract;

import java.util.List;

/** Cuerpo JSON de error que devuelven todos los servicios. */
public record ApiError(String timestamp, int status, String error, List<String> messages, String path) {
}
