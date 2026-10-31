package co.unicauca.saberpro.questions.testdoubles;

import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import co.unicauca.saberpro.questions.domain.model.QuestionData;

import java.util.List;

/** Datos de prueba reutilizables. */
public final class QuestionFixtures {

    private QuestionFixtures() {
    }

    public static QuestionData valid(String author) {
        return new QuestionData("MULTIPLE_CHOICE",
                "Una empresa necesita escalar el envío de notificaciones de forma independiente.",
                "¿Qué estilo arquitectónico favorece mejor esta necesidad?",
                List.of("Monolito en capas", "Microservicios con eventos", "Cliente-servidor de dos capas", "Pipe and filter"),
                "Microservicios con eventos",
                "Los microservicios permiten escalar de forma independiente cada componente.",
                "Richards, M. Fundamentals of Software Architecture, 2020.",
                "Diseño de software", "Arquitectura", "Estilos arquitectónicos", DifficultyLevel.MEDIO, author);
    }

    public static QuestionData withStatement(QuestionData d, String statement) {
        return new QuestionData(d.type(), d.context(), statement, d.options(), d.correctAnswer(), d.justification(),
                d.bibliography(), d.competence(), d.topic(), d.subtopic(), d.difficulty(), d.authorLogin());
    }

    public static QuestionData withOptions(QuestionData d, List<String> options, String correct) {
        return new QuestionData(d.type(), d.context(), d.statement(), options, correct, d.justification(),
                d.bibliography(), d.competence(), d.topic(), d.subtopic(), d.difficulty(), d.authorLogin());
    }
}
