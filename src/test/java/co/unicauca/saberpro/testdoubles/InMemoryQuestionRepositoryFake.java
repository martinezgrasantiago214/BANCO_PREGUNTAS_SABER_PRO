package co.unicauca.saberpro.testdoubles;

import co.unicauca.saberpro.access.IQuestionRepository;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.service.PagedResult;
import co.unicauca.saberpro.service.QuestionFilterCriteria;

import java.util.*;

/** Doble de prueba en memoria para IQuestionRepository, usado en las pruebas unitarias. */
public class InMemoryQuestionRepositoryFake implements IQuestionRepository {

    private final Map<String, Question> questions = new LinkedHashMap<>();

    @Override
    public boolean save(Question question) {
        questions.put(question.getId(), question);
        return true;
    }

    @Override
    public boolean update(Question question) {
        if (!questions.containsKey(question.getId())) {
            return false;
        }
        questions.put(question.getId(), question);
        return true;
    }

    @Override
    public Optional<Question> findById(String id) {
        return Optional.ofNullable(questions.get(id));
    }

    @Override
    public List<Question> findAll() {
        return new ArrayList<>(questions.values());
    }

    @Override
    public List<Question> findByState(QuestionState state) {
        List<Question> result = new ArrayList<>();
        for (Question q : questions.values()) {
            if (q.getState() == state) {
                result.add(q);
            }
        }
        return result;
    }

    @Override
    public PagedResult<Question> search(QuestionFilterCriteria criteria) {
        List<Question> matched = new ArrayList<>();
        for (Question q : questions.values()) {
            if (criteria.getAuthorLogin() != null && !criteria.getAuthorLogin().equals(q.getAuthorLogin())) {
                continue;
            }
            if (criteria.getState() != null && criteria.getState() != q.getState()) {
                continue;
            }
            if (criteria.getTopic() != null && !criteria.getTopic().isBlank()
                    && (q.getTopic() == null || !q.getTopic().contains(criteria.getTopic()))) {
                continue;
            }
            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()
                    && !q.getDirectQuestion().contains(criteria.getKeyword())) {
                continue;
            }
            matched.add(q);
        }
        int from = Math.min((criteria.getPage() - 1) * criteria.getPageSize(), matched.size());
        int to = Math.min(from + criteria.getPageSize(), matched.size());
        return new PagedResult<>(matched.subList(from, to), criteria.getPage(), criteria.getPageSize(), matched.size());
    }

    @Override
    public String getLastError() {
        return null;
    }
}
