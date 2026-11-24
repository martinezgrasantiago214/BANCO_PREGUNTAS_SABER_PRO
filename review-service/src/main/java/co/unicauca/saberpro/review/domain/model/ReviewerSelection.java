package co.unicauca.saberpro.review.domain.model;

import co.unicauca.saberpro.review.domain.exception.ReviewRuleViolationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Objeto de valor (HU04): los logins que el administrador eligio en una
 * asignacion. Solo existe si cumple la regla de 1 a 3 revisores distintos;
 * asi ninguna capa puede trabajar con una seleccion invalida.
 */
public record ReviewerSelection(List<String> logins) {

    public static final int MIN_REVIEWERS = 1;
    public static final int MAX_REVIEWERS = 3;

    public ReviewerSelection {
        List<String> clean = logins == null ? List.of()
                : logins.stream().map(login -> login == null ? null : login.trim()).toList();
        List<String> errors = validate(clean);
        if (!errors.isEmpty()) {
            throw new ReviewRuleViolationException(errors);
        }
        logins = List.copyOf(clean);
    }

    /** @return los errores de la seleccion; vacia si es valida. */
    public static List<String> validate(List<String> logins) {
        List<String> errors = new ArrayList<>();
        if (logins == null || logins.isEmpty()) {
            errors.add("Debe asignar al menos " + MIN_REVIEWERS + " revisor.");
            return errors;
        }
        if (logins.size() > MAX_REVIEWERS) {
            errors.add("No puede asignar mas de " + MAX_REVIEWERS + " revisores a una pregunta.");
        }
        if (logins.stream().anyMatch(login -> login == null || login.isBlank())) {
            errors.add("Hay un revisor sin login.");
        } else if (new HashSet<>(logins).size() != logins.size()) {
            errors.add("No puede asignar el mismo revisor dos veces.");
        }
        return errors;
    }

    public int size() {
        return logins.size();
    }
}
