package co.unicauca.saberpro.frontend.ui;

import co.unicauca.saberpro.frontend.model.Models.QuestionView;

import java.util.List;

/** Texto del detalle completo de una pregunta (HU03: el autor puede "verlas"). */
public final class QuestionDetailFormatter {

    private QuestionDetailFormatter() {
    }

    public static String format(QuestionView q) {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado: ").append(q.statusLabel()).append('\n');
        sb.append("Tipo: ").append(q.type()).append('\n');
        sb.append("Creada: ").append(q.createdAt()).append("\n\n");
        sb.append("CONTEXTO\n").append(nullToEmpty(q.context())).append("\n\n");
        sb.append("PREGUNTA DIRECTA\n").append(nullToEmpty(q.directQuestion())).append("\n\n");
        sb.append("OPCIONES\n");
        List<String> options = q.distractors() != null ? q.distractors() : List.of();
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            sb.append("  ").append((char) ('A' + i)).append(") ").append(option);
            if (option.equals(q.correctAnswer())) {
                sb.append("   <-- respuesta correcta");
            }
            sb.append('\n');
        }
        sb.append("\nJUSTIFICACION\n").append(nullToEmpty(q.justification())).append("\n\n");
        sb.append("BIBLIOGRAFIA\n").append(nullToEmpty(q.bibliography())).append("\n\n");
        sb.append("Competencia: ").append(nullToEmpty(q.competence())).append('\n');
        sb.append("Tema: ").append(nullToEmpty(q.topic())).append('\n');
        sb.append("Subtema: ").append(nullToEmpty(q.subtopic())).append('\n');
        sb.append("Nivel de dificultad: ").append(q.difficultyLevel() != null ? q.difficultyLevel() : "").append('\n');
        return sb.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
