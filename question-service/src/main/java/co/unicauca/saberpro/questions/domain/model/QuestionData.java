package co.unicauca.saberpro.questions.domain.model;

import java.util.List;

/**
 * Datos capturados por el autor para crear o editar una pregunta (HU01).
 * Es lo que recorre la cadena de validacion estructural antes de construir
 * la entidad Question.
 */
public record QuestionData(
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
        DifficultyLevel difficulty,
        String authorLogin) {

    public QuestionData {
        options = options == null ? List.of() : List.copyOf(options.stream().map(o -> o == null ? "" : o).toList());
    }
}
