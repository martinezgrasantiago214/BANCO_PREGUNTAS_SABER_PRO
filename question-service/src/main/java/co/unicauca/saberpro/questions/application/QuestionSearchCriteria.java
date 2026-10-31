package co.unicauca.saberpro.questions.application;

import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;

/** Filtros y paginacion del listado (HU03). Campos null = sin filtro. */
public record QuestionSearchCriteria(
        String authorLogin,
        QuestionStateType state,
        String competence,
        String topic,
        DifficultyLevel difficulty,
        String keyword,
        int page,
        int size) {

    public static final int MAX_PAGE_SIZE = 50;

    public QuestionSearchCriteria {
        page = Math.max(1, page);
        size = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
    }
}
