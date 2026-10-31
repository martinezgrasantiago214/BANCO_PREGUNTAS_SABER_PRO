package co.unicauca.saberpro.questions.infrastructure.persistence;

import co.unicauca.saberpro.questions.application.PageResult;
import co.unicauca.saberpro.questions.application.QuestionSearchCriteria;
import co.unicauca.saberpro.questions.application.port.QuestionRepositoryPort;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.model.QuestionOption;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adaptador JPA del puerto de persistencia, con mapeo dominio <-> entidad. */
@Component
public class QuestionRepositoryAdapter implements QuestionRepositoryPort {

    private final QuestionJpaRepository jpa;

    public QuestionRepositoryAdapter(QuestionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public Question save(Question q) {
        QuestionEntity e = jpa.findById(q.getId()).orElseGet(QuestionEntity::new);
        if (q.getVersion() != null && !q.getVersion().equals(e.getVersion())) {
            throw new OptimisticLockingFailureException("La pregunta " + q.getId() + " cambió mientras se editaba.");
        }
        e.setId(q.getId());
        e.setType(q.getType());
        e.setAuthorLogin(q.getAuthorLogin());
        e.setContext(q.getContext());
        e.setStatement(q.getStatement());
        e.getOptions().clear();
        q.getOptions().forEach(o -> e.getOptions().add(new OptionEmbeddable(o.text(), o.correct(), o.order())));
        e.setJustification(q.getJustification());
        e.setBibliography(q.getBibliography());
        e.setCompetence(q.getCompetence());
        e.setTopic(q.getTopic());
        e.setSubtopic(q.getSubtopic());
        e.setDifficulty(q.getDifficulty());
        e.setState(q.getStateType());
        e.setCreatedAt(q.getCreatedAt());
        e.setUpdatedAt(q.getUpdatedAt());
        return toDomain(jpa.saveAndFlush(e));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Question> findById(String id) {
        return jpa.findById(id).map(QuestionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional
    public void delete(String id) {
        jpa.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Question> search(QuestionSearchCriteria c) {
        PageRequest pageable = PageRequest.of(c.page() - 1, c.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<QuestionEntity> page = jpa.findAll(QuestionSpecifications.from(c), pageable);
        return new PageResult<>(page.getContent().stream().map(QuestionRepositoryAdapter::toDomain).toList(),
                c.page(), c.size(), page.getTotalElements());
    }

    static Question toDomain(QuestionEntity e) {
        return Question.builder()
                .id(e.getId())
                .type(e.getType())
                .authorLogin(e.getAuthorLogin())
                .context(e.getContext())
                .statement(e.getStatement())
                .options(e.getOptions().stream()
                        .map(o -> new QuestionOption(o.getText(), o.isCorrect(), o.getPosition()))
                        .toList())
                .justification(e.getJustification())
                .bibliography(e.getBibliography())
                .competence(e.getCompetence())
                .topic(e.getTopic())
                .subtopic(e.getSubtopic())
                .difficulty(e.getDifficulty())
                .state(e.getState())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .version(e.getVersion())
                .build();
    }
}
