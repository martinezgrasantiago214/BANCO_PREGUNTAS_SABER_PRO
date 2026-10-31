package co.unicauca.saberpro.questions.infrastructure.persistence;

import co.unicauca.saberpro.questions.application.QuestionSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Traduce los filtros de HU03 a una consulta JPA Criteria. */
final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    static Specification<QuestionEntity> from(QuestionSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (notBlank(c.authorLogin())) {
                predicates.add(cb.equal(root.get("authorLogin"), c.authorLogin()));
            }
            if (c.state() != null) {
                predicates.add(cb.equal(root.get("state"), c.state()));
            }
            if (c.difficulty() != null) {
                predicates.add(cb.equal(root.get("difficulty"), c.difficulty()));
            }
            if (notBlank(c.competence())) {
                predicates.add(cb.like(cb.lower(root.get("competence")), like(c.competence())));
            }
            if (notBlank(c.topic())) {
                predicates.add(cb.like(cb.lower(root.get("topic")), like(c.topic())));
            }
            if (notBlank(c.keyword())) {
                String k = like(c.keyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("statement")), k),
                        cb.like(cb.lower(root.get("context")), k),
                        cb.like(cb.lower(root.get("subtopic")), k)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean notBlank(String v) {
        return v != null && !v.isBlank();
    }

    private static String like(String v) {
        return "%" + v.trim().toLowerCase() + "%";
    }
}
