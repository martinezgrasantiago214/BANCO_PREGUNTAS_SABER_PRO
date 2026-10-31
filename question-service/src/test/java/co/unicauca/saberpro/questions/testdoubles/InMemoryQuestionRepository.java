package co.unicauca.saberpro.questions.testdoubles;

import co.unicauca.saberpro.questions.application.PageResult;
import co.unicauca.saberpro.questions.application.QuestionSearchCriteria;
import co.unicauca.saberpro.questions.application.port.QuestionRepositoryPort;
import co.unicauca.saberpro.questions.domain.model.Question;

import java.util.*;

public class InMemoryQuestionRepository implements QuestionRepositoryPort {

    private final Map<String, Question> data = new LinkedHashMap<>();
    public int saves;

    @Override
    public Question save(Question question) {
        saves++;
        data.put(question.getId(), question);
        return question;
    }

    @Override
    public Optional<Question> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public void delete(String id) {
        data.remove(id);
    }

    @Override
    public PageResult<Question> search(QuestionSearchCriteria c) {
        List<Question> filtered = data.values().stream()
                .filter(q -> c.authorLogin() == null || q.getAuthorLogin().equals(c.authorLogin()))
                .filter(q -> c.state() == null || q.getStateType() == c.state())
                .filter(q -> c.keyword() == null
                        || q.getStatement().toLowerCase().contains(c.keyword().toLowerCase()))
                .toList();
        int from = Math.min((c.page() - 1) * c.size(), filtered.size());
        int to = Math.min(from + c.size(), filtered.size());
        return new PageResult<>(filtered.subList(from, to), c.page(), c.size(), filtered.size());
    }
}
