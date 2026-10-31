package co.unicauca.saberpro.question;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.question.domain.QuestionRequest;

import java.util.Arrays;

/** Datos de prueba compartidos por las pruebas del question-service. */
public final class TestData {

    private TestData() {
    }

    public static QuestionRequest validRequest(String type) {
        return new QuestionRequest(type,
                "Una empresa necesita que su banco de preguntas admita nuevos tipos.",
                "¿Que patron permite agregar funcionalidad mediante plugins?",
                Arrays.asList("Microkernel", "Cliente-Servidor", "Capas", "Broker"), "Microkernel",
                "El patron Microkernel separa un nucleo minimo de sus plugins.",
                "Richards, M. Software Architecture Patterns. O'Reilly, 2015.",
                "Arquitectura de software", "Patrones arquitectonicos", "Microkernel",
                DifficultyLevel.MEDIO, "autor1");
    }

    public static QuestionRequest withCorrectAnswer(QuestionRequest r, String correctAnswer) {
        return new QuestionRequest(r.getType(), r.getContext(), r.getDirectQuestion(), r.getDistractors(),
                correctAnswer, r.getJustification(), r.getBibliography(), r.getCompetence(), r.getTopic(),
                r.getSubtopic(), r.getDifficultyLevel(), r.getAuthorLogin());
    }
}
