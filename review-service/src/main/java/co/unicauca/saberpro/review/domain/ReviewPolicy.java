package co.unicauca.saberpro.review.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Regla de negocio de HU04: a cada pregunta se le asignan entre 1 y 3 revisores distintos. */
public final class ReviewPolicy {

    public static final int MIN_REVIEWERS = 1;
    public static final int MAX_REVIEWERS = 3;

    private ReviewPolicy() {
    }

    /** @return la lista de errores; vacia si la seleccion de revisores es valida. */
    public static List<String> validateReviewers(List<String> reviewerLogins) {
        List<String> errors = new ArrayList<>();
        if (reviewerLogins == null || reviewerLogins.isEmpty()) {
            errors.add("Debe asignar al menos " + MIN_REVIEWERS + " revisor.");
            return errors;
        }
        if (reviewerLogins.size() > MAX_REVIEWERS) {
            errors.add("No puede asignar mas de " + MAX_REVIEWERS + " revisores a una pregunta.");
        }
        if (reviewerLogins.stream().anyMatch(login -> login == null || login.isBlank())) {
            errors.add("Hay un revisor sin login.");
        } else if (new HashSet<>(reviewerLogins).size() != reviewerLogins.size()) {
            errors.add("No puede asignar el mismo revisor dos veces.");
        }
        return errors;
    }
}
