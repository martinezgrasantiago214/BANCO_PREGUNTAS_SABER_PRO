package co.unicauca.saberpro.question.access;

import co.unicauca.saberpro.question.domain.Question;

import java.util.Optional;

/**
 * Puerto de persistencia de preguntas (DIP): el servicio de aplicacion
 * depende de esta interfaz, no de SQLite.
 */
public interface IQuestionRepository {

    /** @return true si la pregunta quedo guardada. */
    boolean save(Question question);

    /** @return true si la pregunta quedo actualizada (contenido y estado). */
    boolean update(Question question);

    Optional<Question> findById(String id);

    /** HU03: listado filtrado y paginado. */
    PagedResult<Question> search(QuestionFilterCriteria criteria);
}
