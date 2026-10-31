package co.unicauca.saberpro.questions.infrastructure.web.dto;

import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.List;

/** Cuerpo JSON para crear/editar una pregunta (HU01). */
public record QuestionRequest(
        String type,
        String context,
        String statement,
        List<String> options,
        String correctAnswer,
        String justification,
        String bibliography,
        String competence,
        String topic,
        String subtopic,
        DifficultyLevel difficulty) {

    public QuestionData toData(String authorLogin) {
        return new QuestionData(type, context, statement, options, correctAnswer, justification,
                bibliography, competence, topic, subtopic, difficulty, authorLogin);
    }
}
