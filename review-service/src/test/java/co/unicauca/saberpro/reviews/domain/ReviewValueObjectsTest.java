package co.unicauca.saberpro.reviews.domain;

import co.unicauca.saberpro.reviews.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewValueObjectsTest {

    @Test
    void scoreMustBeBetweenOneAndFive() {
        assertThrows(IllegalArgumentException.class, () -> new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 6, ""));
        assertThrows(IllegalArgumentException.class, () -> new CriterionScore(null, 3, ""));
        assertEquals("", new CriterionScore(ReviewCriterion.CLARIDAD_ENUNCIADO, 5, null).comment());
    }

    @Test
    void observationRequiresTextAndLimitsLength() {
        assertThrows(IllegalArgumentException.class, () -> new Observation("rev1", "  ", null));
        assertThrows(IllegalArgumentException.class, () -> new Observation("rev1", "x".repeat(2001), null));
        Observation o = new Observation("rev1", "  Mejorar distractor C  ", null);
        assertEquals("Mejorar distractor C", o.text());
        assertNotNull(o.createdAt());
    }

    @Test
    void enumsHaveLabels() {
        for (ReviewCriterion c : ReviewCriterion.values()) {
            assertFalse(c.getLabel().isBlank());
        }
        assertEquals("Pendiente de asignación", ReviewStatus.PENDIENTE_ASIGNACION.getLabel());
    }
}
