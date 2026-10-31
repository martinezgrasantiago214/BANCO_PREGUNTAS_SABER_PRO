package co.unicauca.saberpro.questions.domain;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.model.QuestionData;
import co.unicauca.saberpro.questions.domain.model.QuestionOption;
import co.unicauca.saberpro.questions.domain.plugin.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.questions.testdoubles.QuestionFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionTest {

    private final MultipleChoiceQuestionPlugin plugin = new MultipleChoiceQuestionPlugin();

    private Question newQuestion() {
        return plugin.generate(QuestionFixtures.valid("autor1"));
    }

    @Test
    void builderCreatesDraftQuestion() {
        Question q = Question.builder().id("1").type("MULTIPLE_CHOICE").authorLogin("autor1")
                .statement("Enunciado de prueba").option("A", false).option("B", true)
                .difficulty(DifficultyLevel.ALTO).build();
        assertEquals(QuestionStateType.BORRADOR, q.getStateType());
        assertEquals("B", q.getCorrectAnswer());
        assertEquals(List.of("A", "B"), q.getOptionTexts());
        assertEquals(2, q.getOptions().get(1).order());
        assertNotNull(q.getCreatedAt());
    }

    @Test
    void builderRejectsMoreThanOneCorrectOption() {
        Question.Builder b = Question.builder().id("1").type("T").authorLogin("a").option("A", true).option("B", true);
        assertThrows(IllegalStateException.class, b::build);
    }

    @Test
    void builderRequiresIdTypeAndAuthor() {
        assertThrows(NullPointerException.class, () -> Question.builder().type("T").authorLogin("a").build());
    }

    @Test
    void optionRequiresText() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionOption(" ", false, 1));
    }

    @Test
    void lifecycleDelegatesToState() {
        Question q = newQuestion();
        q.submitForReview();
        assertEquals(QuestionStateType.PENDIENTE_REVISION, q.getStateType());
        assertFalse(q.isEditable());
        q.startReview();
        q.reject();
        assertEquals(QuestionStateType.RECHAZADA, q.getStateType());
        assertTrue(q.isEditable());
    }

    @Test
    void editingRejectedQuestionReturnsItToDraft() {
        Question q = newQuestion();
        q.submitForReview();
        q.startReview();
        q.reject();
        QuestionData changed = QuestionFixtures.withStatement(QuestionFixtures.valid("autor1"),
                "¿Qué patrón desacopla mejor productores y consumidores?");
        q.edit(changed, plugin.buildOptions(changed));
        assertEquals(QuestionStateType.BORRADOR, q.getStateType());
        assertTrue(q.getStatement().startsWith("¿Qué patrón"));
    }

    @Test
    void cannotEditOrDeleteWhileInReview() {
        Question q = newQuestion();
        q.submitForReview();
        QuestionData data = QuestionFixtures.valid("autor1");
        assertThrows(QuestionDomainException.class, () -> q.edit(data, plugin.buildOptions(data)));
        assertThrows(QuestionDomainException.class, q::ensureDeletable);
    }

    @Test
    void knowsItsOwner() {
        Question q = newQuestion();
        assertTrue(q.isOwnedBy("autor1"));
        assertFalse(q.isOwnedBy("autor2"));
    }
}
