package co.unicauca.saberpro.question.domain.validation;

import co.unicauca.saberpro.question.domain.QuestionRequest;

import java.util.List;

/** Valida que la respuesta correcta exista y sea uno de los distractores. */
public class CorrectAnswerValidationFilter implements QuestionFilter {

    @Override
    public boolean process(QuestionRequest request) {
        String correctAnswer = request.getCorrectAnswer();
        List<String> options = request.getDistractors();

        if (correctAnswer == null || correctAnswer.trim().isEmpty() || options == null) {
            return false;
        }

        for (String option : options) {
            if (option != null && option.trim().equalsIgnoreCase(correctAnswer.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getErrorMessage() {
        return "La respuesta correcta debe existir y coincidir exactamente con uno de los distractores.";
    }
}
