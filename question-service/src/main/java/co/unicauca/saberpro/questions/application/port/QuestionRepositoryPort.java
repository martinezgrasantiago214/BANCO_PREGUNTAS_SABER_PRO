package co.unicauca.saberpro.questions.application.port;

import co.unicauca.saberpro.questions.application.PageResult;
import co.unicauca.saberpro.questions.application.QuestionSearchCriteria;
import co.unicauca.saberpro.questions.domain.model.Question;

import java.util.Optional;

/** Puerto de salida de persistencia. */
public interface QuestionRepositoryPort {

    Question save(Question question);

    Optional<Question> findById(String id);

    void delete(String id);

    PageResult<Question> search(QuestionSearchCriteria criteria);
}
