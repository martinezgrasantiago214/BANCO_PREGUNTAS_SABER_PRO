package co.unicauca.saberpro.questions.domain;

import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import co.unicauca.saberpro.questions.domain.model.QuestionData;
import co.unicauca.saberpro.questions.domain.validation.*;
import co.unicauca.saberpro.questions.testdoubles.QuestionFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de la cadena de responsabilidad de validacion estructural. */
class QuestionValidationChainTest {

    private final QuestionValidationHandler chain = QuestionValidationChain.standard();

    @Test
    void validQuestionPassesAllRules() {
        assertTrue(chain.validate(QuestionFixtures.valid("autor1")).isValid());
    }

    @Test
    void shortStatementFailsContentRule() {
        QuestionData d = QuestionFixtures.withStatement(QuestionFixtures.valid("a"), "corta");
        assertEquals(1, new ContentRule().validate(d).getErrors().size());
    }

    @Test
    void optionsMustBeFourDistinctAndNotBlank() {
        QuestionData base = QuestionFixtures.valid("a");
        assertFalse(new OptionsRule().validate(QuestionFixtures.withOptions(base, List.of("A", "B", "C"), "A")).isValid());
        assertFalse(new OptionsRule().validate(QuestionFixtures.withOptions(base, List.of("A", "B", "C", "a"), "A")).isValid());
        assertFalse(new OptionsRule().validate(QuestionFixtures.withOptions(base, List.of("A", "B", "C", " "), "A")).isValid());
    }

    @Test
    void correctAnswerMustMatchAnOption() {
        QuestionData d = QuestionFixtures.withOptions(QuestionFixtures.valid("a"), List.of("A", "B", "C", "D"), "E");
        assertFalse(new CorrectAnswerRule().validate(d).isValid());
    }

    @Test
    void classificationAndEvidenceAreRequired() {
        QuestionData d = new QuestionData("MULTIPLE_CHOICE", "ctx", "Pregunta suficientemente larga",
                List.of("A", "B", "C", "D"), "A", "", "", "", "T", "S", null, "");
        assertFalse(new ClassificationRule().validate(d).isValid());
        assertFalse(new EvidenceMetadataRule().validate(d).isValid());
    }

    @Test
    void chainCollectsEveryErrorInsteadOfStoppingAtTheFirst() {
        QuestionData empty = new QuestionData(null, null, null, null, null, null, null, null, null, null, null, null);
        ValidationResult result = chain.validate(empty);
        assertEquals(5, result.getErrors().size());
    }

    @Test
    void validDifficultyLabels() {
        assertEquals("Medio", DifficultyLevel.MEDIO.getLabel());
    }
}
