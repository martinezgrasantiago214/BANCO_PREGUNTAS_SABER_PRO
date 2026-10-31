package co.unicauca.saberpro.questions.application;

import co.unicauca.saberpro.questions.application.port.QuestionEventPublisherPort;
import co.unicauca.saberpro.questions.application.port.QuestionRepositoryPort;
import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.Question;
import co.unicauca.saberpro.questions.domain.model.QuestionData;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypePlugin;
import co.unicauca.saberpro.questions.domain.plugin.QuestionTypeRegistry;

/**
 * Casos de uso del contexto de preguntas (HU01, HU02, HU03) y reaccion a
 * los eventos del contexto de revision (HU04, HU05).
 */
public class QuestionService {

    private final QuestionRepositoryPort repository;
    private final QuestionEventPublisherPort publisher;
    private final QuestionTypeRegistry registry;

    public QuestionService(QuestionRepositoryPort repository, QuestionEventPublisherPort publisher,
                           QuestionTypeRegistry registry) {
        this.repository = repository;
        this.publisher = publisher;
        this.registry = registry;
    }

    /** HU01: crea la pregunta en Borrador tras la validacion estructural. */
    public Question create(QuestionData data) {
        Question question = registry.pluginFor(data.type()).generate(data);
        return repository.save(question);
    }

    /** HU03: edicion por el autor (solo en Borrador o Rechazada). */
    public Question update(String id, QuestionData data, String requesterLogin) {
        Question question = getOwned(id, requesterLogin);
        if (!question.isEditable()) {
            throw new QuestionDomainException(QuestionDomainException.Kind.INVALID_TRANSITION,
                    "Solo se pueden editar preguntas en Borrador o Rechazada.");
        }
        QuestionData normalized = withAuthor(data, question.getAuthorLogin(), question.getType());
        QuestionTypePlugin plugin = registry.pluginFor(question.getType());
        plugin.validate(normalized);
        question.edit(normalized, plugin.buildOptions(normalized));
        return repository.save(question);
    }

    public void delete(String id, String requesterLogin) {
        Question question = getOwned(id, requesterLogin);
        question.ensureDeletable();
        repository.delete(id);
    }

    /** HU02: Borrador -> Pendiente de revision, y se avisa al contexto de revision. */
    public Question submitForReview(String id, String requesterLogin) {
        Question question = getOwned(id, requesterLogin);
        question.submitForReview();
        Question saved = repository.save(question);
        publisher.publishSubmittedForReview(saved);
        return saved;
    }

    public Question findById(String id) {
        return repository.findById(id).orElseThrow(() -> QuestionDomainException.notFound(id));
    }

    /** HU03: listado paginado y filtrado. */
    public PageResult<Question> search(QuestionSearchCriteria criteria) {
        return repository.search(criteria);
    }

    /** Reaccion a ReviewersAssignedEvent. Idempotente: un evento repetido no falla. */
    public void markInReview(String id) {
        Question question = findById(id);
        if (question.getStateType() == QuestionStateType.PENDIENTE_REVISION) {
            question.startReview();
            repository.save(question);
        }
    }

    /**
     * Reaccion a ReviewCompletedEvent. Idempotente y tolerante al desorden:
     * si el evento de asignacion aun no se proceso, primero inicia la revision.
     */
    public void applyReviewDecision(String id, boolean approved) {
        Question question = findById(id);
        QuestionStateType target = approved ? QuestionStateType.APROBADA : QuestionStateType.RECHAZADA;
        if (question.getStateType() == target) {
            return;
        }
        if (question.getStateType() == QuestionStateType.PENDIENTE_REVISION) {
            question.startReview();
        }
        if (approved) {
            question.approve();
        } else {
            question.reject();
        }
        repository.save(question);
    }

    public QuestionTypeRegistry getRegistry() {
        return registry;
    }

    private Question getOwned(String id, String requesterLogin) {
        Question question = findById(id);
        if (!question.isOwnedBy(requesterLogin)) {
            throw QuestionDomainException.forbidden("Solo el autor de la pregunta puede realizar esta acción.");
        }
        return question;
    }

    private static QuestionData withAuthor(QuestionData d, String author, String type) {
        return new QuestionData(type, d.context(), d.statement(), d.options(), d.correctAnswer(),
                d.justification(), d.bibliography(), d.competence(), d.topic(), d.subtopic(),
                d.difficulty(), author);
    }
}
