package co.unicauca.saberpro.review.application.service;

import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;

/** Utilidad de los casos de uso para los parametros de texto obligatorios. */
final class Texts {

    private Texts() {
    }

    static String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ReviewRuleViolationException(message);
        }
        return value.trim();
    }
}
