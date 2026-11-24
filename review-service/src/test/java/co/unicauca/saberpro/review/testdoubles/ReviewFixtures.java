package co.unicauca.saberpro.review.testdoubles;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.review.domain.model.QuestionSubmission;
import co.unicauca.saberpro.review.domain.model.ReviewerCandidate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Datos de prueba compartidos por las pruebas del review-service. */
public final class ReviewFixtures {

    public static final Instant T0 = Instant.parse("2026-11-20T15:00:00Z");
    public static final String QUESTION = "q-1";
    public static final String AUTHOR = "autor1";
    public static final String ADMIN = "admin";

    /** Usuarios del directorio y su rol (lo que responderia el user-service / Keycloak). */
    public static final Map<String, Role> USERS = Map.of(
            "rev1", Role.REVISOR, "rev2", Role.REVISOR, "rev3", Role.REVISOR, "rev4", Role.REVISOR,
            AUTHOR, Role.AUTOR_PREGUNTAS, ADMIN, Role.ADMINISTRADOR);

    private ReviewFixtures() {
    }

    public static QuestionSubmission submission(int version) {
        return new QuestionSubmission(QUESTION, version, AUTHOR, "¿Que patron desacopla al emisor del receptor? v"
                + version, T0.plusSeconds(3600L * (version - 1)));
    }

    public static ReviewerCandidate candidate(String login) {
        Role role = USERS.get(login);
        return role == null ? ReviewerCandidate.unknown(login) : ReviewerCandidate.of(login, role);
    }

    public static List<ReviewerCandidate> candidates(String... logins) {
        return java.util.Arrays.stream(logins).map(ReviewFixtures::candidate).toList();
    }
}
