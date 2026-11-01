package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.contract.EvaluationVerdict;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ConflictException;
import co.unicauca.saberpro.common.web.ForbiddenOperationException;
import co.unicauca.saberpro.common.web.PendingIssueException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.question.access.IQuestionRepository;
import co.unicauca.saberpro.question.access.PagedResult;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.domain.Question;
import co.unicauca.saberpro.question.domain.QuestionRequest;
import co.unicauca.saberpro.question.domain.factory.QuestionFactory;
import co.unicauca.saberpro.question.domain.factory.QuestionFactoryRegistry;
import co.unicauca.saberpro.common.events.DomainEventBus;

import java.util.List;

/**
 * Casos de uso del question-service. Coordina el dominio (fabricas, Builder,
 * estados), la persistencia y la publicacion de eventos; no contiene reglas
 * de negocio propias.
 *
 * Los casos de uso marcados con TODO(PSP-xx) quedan para el issue indicado:
 * responden 501 Not Implemented hasta que se implementen. Las piezas que
 * necesitan (fabricas + validacion, repositorio, estados, bus de eventos)
 * ya estan disponibles y probadas.
 */
public class QuestionApplicationService {

    private final IQuestionRepository repository;
    private final QuestionFactoryRegistry factories;
    private final DomainEventBus eventBus;

    public QuestionApplicationService(IQuestionRepository repository, QuestionFactoryRegistry factories,
                                      DomainEventBus eventBus) {
        this.repository = repository;
        this.factories = factories;
        this.eventBus = eventBus;
    }

    /**
     * HU01: crea una pregunta en estado Borrador.
     *
     * TODO(PSP-16, PSP-17, PSP-18):
     *  1. Elegir la fabrica con factories.forType(request.getType()); si no hay, BusinessException.
     *  2. factory.create(request): aplica la validacion estructural (PSP-17) y construye la pregunta.
     *  3. Guardar con repository.save(...) (PSP-18).
     *  4. Publicar new QuestionCreatedEvent(...) en eventBus.
     */
    public Question create(QuestionRequest request) {
        throw new PendingIssueException("PSP-18", "crear y almacenar preguntas (HU01)");
    }

    /** Consulta una pregunta por su id (la usan el autor para editar y el revisor para leerla). */
    public Question findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la pregunta " + id));
    }

    /** Tamano maximo de pagina: ningun listado trae todas las preguntas de una vez. */
    public static final int MAX_PAGE_SIZE = 50;

    /**
     * HU03: listado de las preguntas del autor con busqueda, filtros y
     * paginacion (PSP-24 listado, PSP-25 busqueda por palabra clave en el
     * enunciado, el contexto o el tema, PSP-26
     * filtros por estado y tema, PSP-27 paginacion).
     *
     * Normaliza los criterios (textos sin espacios sobrantes, tamano de
     * pagina entre 1 y {@link #MAX_PAGE_SIZE}) y, si la pagina pedida ya no
     * existe (por ejemplo, despues de aplicar un filtro), devuelve la ultima.
     */
    public PagedResult<Question> search(QuestionFilterCriteria criteria) {
        QuestionFilterCriteria normalized = new QuestionFilterCriteria()
                .setAuthorLogin(blankToNull(criteria.getAuthorLogin()))
                .setStatus(criteria.getStatus())
                .setTopic(blankToNull(criteria.getTopic()))
                .setKeyword(blankToNull(criteria.getKeyword()))
                .setPage(criteria.getPage())
                .setPageSize(Math.min(criteria.getPageSize(), MAX_PAGE_SIZE));

        PagedResult<Question> result = repository.search(normalized);
        if (result.items().isEmpty() && result.totalItems() > 0 && normalized.getPage() > result.totalPages()) {
            return repository.search(normalized.setPage(result.totalPages()));
        }
        return result;
    }

    /**
     * HU03: edita una pregunta propia (PSP-28).
     *
     * Solo el autor puede editarla (si no, 403) y solo mientras su estado lo
     * permita (Borrador, lo decide el patron State con isEditable(); si no, 409). Los datos
     * nuevos pasan por la misma validacion estructural de la fabrica del
     * tipo. La pregunta conserva su id, tipo, autor, fecha de creacion y
     * estado.
     */
    public Question update(String id, QuestionRequest request) {
        Question current = findById(id);
        if (!current.isAuthoredBy(request.getAuthorLogin())) {
            throw new ForbiddenOperationException("Solo el autor de la pregunta puede editarla.");
        }
        if (!current.isEditable()) {
            throw new ConflictException("Solo se pueden editar preguntas en estado Borrador. Esta pregunta esta en "
                    + current.getStatus().getLabel() + ".");
        }

        QuestionRequest data = new QuestionRequest(current.getType(), request.getContext(),
                request.getDirectQuestion(), request.getDistractors(), request.getCorrectAnswer(),
                request.getJustification(), request.getBibliography(), request.getCompetence(),
                request.getTopic(), request.getSubtopic(), request.getDifficultyLevel(), current.getAuthorLogin());
        factories.forType(current.getType())
                .orElseThrow(() -> new BusinessException("No hay una fabrica para el tipo " + current.getType()))
                .validate(data);

        Question updated = Question.builder()
                .id(current.getId())
                .type(current.getType())
                .fromRequest(data)
                .createdAt(current.getCreatedAt())
                .status(current.getStatus())
                .build();
        if (!repository.update(updated)) {
            throw new BusinessException("No fue posible guardar los cambios de la pregunta " + id + ".");
        }
        return updated;
    }

    /**
     * HU02: Borrador -> Pendiente de revision.
     *
     * TODO(PSP-21): verificar que quien lo pide es el autor, llamar question.submitForReview() (el estado
     * decide si se puede), repository.update(...) y publicar QuestionSubmittedForReviewEvent en eventBus.
     */
    public Question submitForReview(String id, String authorLogin) {
        throw new PendingIssueException("PSP-21", "enviar una pregunta a revision (HU02)");
    }

    /**
     * HU05 (PSP-48): aplica el veredicto de un revisor, que llega en el evento
     * review.evaluated del review-service.
     *
     * El patron State decide si la transicion es valida: solo una pregunta
     * Pendiente de revision se puede aprobar o rechazar; en otro estado se
     * lanza InvalidStateTransitionException.
     *
     * Es idempotente: RabbitMQ puede entregar el mismo evento mas de una vez,
     * y si la pregunta ya tiene el estado del veredicto no se hace nada.
     *
     * @return la pregunta con su estado final.
     */
    public synchronized Question applyEvaluation(QuestionEvaluatedEvent event) {
        if (event == null || event.questionId() == null || event.verdict() == null) {
            throw new BusinessException("El evento de evaluacion no trae la pregunta o el veredicto.");
        }
        Question question = findById(event.questionId());
        boolean approve = event.verdict() == EvaluationVerdict.APROBADA;
        QuestionStatus target = approve ? QuestionStatus.APROBADA : QuestionStatus.RECHAZADA;
        if (question.getStatus() == target) {
            return question;
        }

        if (approve) {
            question.approve();
        } else {
            question.reject();
        }
        if (!repository.update(question)) {
            throw new IllegalStateException("No fue posible guardar el nuevo estado de la pregunta "
                    + question.getId() + ".");
        }
        return question;
    }

    /** Tipos de pregunta disponibles (fabricas cargadas por reflexion). */
    public List<QuestionFactory> availableTypes() {
        return factories.all();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
