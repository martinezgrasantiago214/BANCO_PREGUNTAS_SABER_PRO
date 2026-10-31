package co.unicauca.saberpro.question.application;

import co.unicauca.saberpro.common.events.QuestionEvaluatedEvent;
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

    /**
     * HU03: listado de las preguntas del autor con filtros y paginacion.
     *
     * TODO(PSP-24, PSP-25, PSP-26, PSP-27): validar los criterios y delegar en repository.search(criteria),
     * que ya filtra por autor, estado, tema y palabra clave, y pagina.
     */
    public PagedResult<Question> search(QuestionFilterCriteria criteria) {
        throw new PendingIssueException("PSP-24", "listar, buscar, filtrar y paginar preguntas (HU03)");
    }

    /**
     * HU03: edita una pregunta propia.
     *
     * TODO(PSP-28): solo el autor y solo si question.isEditable() (estado Borrador); los datos nuevos pasan
     * por la misma validacion de la fabrica del tipo antes de repository.update(...).
     */
    public Question update(String id, QuestionRequest request) {
        throw new PendingIssueException("PSP-28", "editar preguntas (HU03)");
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
     * HU05: aplica el veredicto de un revisor (evento que publica el review-service).
     *
     * TODO(PSP-48): buscar la pregunta, llamar question.approve() o question.reject() segun
     * event.verdict(), y repository.update(...).
     */
    public void applyEvaluation(QuestionEvaluatedEvent event) {
        throw new PendingIssueException("PSP-48", "cambiar el estado a Aprobada/Rechazada (HU05)");
    }

    /** Tipos de pregunta disponibles (fabricas cargadas por reflexion). */
    public List<QuestionFactory> availableTypes() {
        return factories.all();
    }
}
