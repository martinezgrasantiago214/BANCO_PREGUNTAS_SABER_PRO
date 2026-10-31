package co.unicauca.saberpro.questions.domain;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.plugin.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypeRegistry;
import co.unicauca.saberpro.questions.testdoubles.QuestionFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Factory Method del plugin y nucleo del microkernel. */
class QuestionPluginTest {

    @Test
    void pluginFactoryMethodBuildsValidatedDraft() {
        Question q = new MultipleChoiceQuestionPlugin().generate(QuestionFixtures.valid("autor1"));
        assertNotNull(q.getId());
        assertEquals(MultipleChoiceQuestionPlugin.TYPE, q.getType());
        assertEquals(QuestionStateType.BORRADOR, q.getStateType());
        assertEquals("Microservicios con eventos", q.getCorrectAnswer());
        assertEquals(1, q.getOptions().stream().filter(o -> o.correct()).count());
    }

    @Test
    void pluginRejectsInvalidDataWithDetails() {
        QuestionDomainException ex = assertThrows(QuestionDomainException.class,
                () -> new MultipleChoiceQuestionPlugin().generate(
                        QuestionFixtures.withStatement(QuestionFixtures.valid("a"), "x")));
        assertEquals(QuestionDomainException.Kind.VALIDATION, ex.getKind());
        assertEquals(1, ex.getDetails().size());
    }

    @Test
    void registryLoadsPluginsByReflection() {
        QuestionTypeRegistry registry = QuestionTypeRegistry.fromClasspath("plugins.properties");
        assertEquals(1, registry.plugins().size());
        assertTrue(registry.pluginFor("multiple_choice") instanceof MultipleChoiceQuestionPlugin);
        assertTrue(registry.pluginFor(null) instanceof MultipleChoiceQuestionPlugin, "tipo por defecto");
    }

    @Test
    void registryRejectsUnknownType() {
        QuestionTypeRegistry registry = new QuestionTypeRegistry(List.of(new MultipleChoiceQuestionPlugin()));
        assertThrows(QuestionDomainException.class, () -> registry.pluginFor("ENSAYO"));
        assertThrows(IllegalStateException.class, () -> QuestionTypeRegistry.fromClasspath("no-existe.properties"));
    }
}
