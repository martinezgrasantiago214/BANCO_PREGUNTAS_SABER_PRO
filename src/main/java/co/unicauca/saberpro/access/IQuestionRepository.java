package co.unicauca.saberpro.access;

import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.service.PagedResult;
import co.unicauca.saberpro.service.QuestionFilterCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Abstraccion (puerto) para la persistencia de preguntas. Principio de
 * Inversion de Dependencias: QuestionService depende de esta interfaz, no
 * de una tecnologia concreta de almacenamiento.
 */
public interface IQuestionRepository {

    /** @return true si la pregunta quedo efectivamente guardada. */
    boolean save(Question question);

    /** @return true si la pregunta quedo efectivamente actualizada. */
    boolean update(Question question);

    Optional<Question> findById(String id);

    List<Question> findAll();

    List<Question> findByState(QuestionState state);

    /** HU03: listado paginado y filtrado de las preguntas de un autor. */
    PagedResult<Question> search(QuestionFilterCriteria criteria);

    /**
     * @return el mensaje de la ultima excepcion de base de datos ocurrida
     * (por ejemplo al listar/buscar), o {@code null} si la ultima operacion
     * no tuvo errores. Permite que la capa de presentacion distinga entre
     * "no hay preguntas" y "hubo un error consultando la base de datos".
     */
    String getLastError();
}