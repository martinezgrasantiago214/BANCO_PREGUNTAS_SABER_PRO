package co.unicauca.saberpro.service;

import co.unicauca.saberpro.access.IQuestionRepository;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.infra.Observer;
import co.unicauca.saberpro.infra.Subject;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de dominio del Banco de Preguntas. Coordina:
 *  - HU01: creacion de preguntas a traves del Microkernel + pipeline de
 *          Tuberias y Filtros (validacion estructural, HU03).
 *  - HU02: cambio de estado "borrador" -> "Pendiente de revision".
 *  - HU03: listado paginado y filtrado de las preguntas de un autor.
 *  - HU04: asignacion de revisores por parte del administrador y envio de
 *          la notificacion por correo.
 *
 * Expone un Subject (patron Observer, micro patron MVC) para que las vistas
 * de la capa de presentacion se refresquen automaticamente ante cualquier
 * cambio en el banco de preguntas.
 */
public class QuestionService {

    private final IQuestionRepository questionRepository;
    private final QuestionMicrokernel microkernel;
    private final UserService userService;
    private final IEmailService emailService;
    private final Subject subject = new Subject();

    public QuestionService(IQuestionRepository questionRepository, QuestionMicrokernel microkernel,
                            UserService userService, IEmailService emailService) {
        this.questionRepository = questionRepository;
        this.microkernel = microkernel;
        this.userService = userService;
        this.emailService = emailService;
    }

    /**
     * HU01: crea una pregunta nueva. El microkernel delega en el plugin que
     * soporte el "type" solicitado, el cual ejecuta su pipeline de
     * validacion estructural (HU03) antes de construir la Question. Si la
     * pregunta es valida queda en estado BORRADOR.
     */
    public OperationResult crearPregunta(QuestionRequest request) {
        Question generated;
        try {
            generated = microkernel.executePlugin(request.getType(), request);
        } catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }

        if (generated == null) {
            return OperationResult.fail("La pregunta no supero la validacion estructural: "
                    + microkernel.getLastError());
        }

        generated.setState(QuestionState.BORRADOR);
        boolean saved = questionRepository.save(generated);
        if (!saved) {
            return OperationResult.fail("La pregunta paso las validaciones pero no se pudo guardar "
                    + "en la base de datos. Revise la consola para ver el detalle del error.");
        }
        subject.notificarObservers();
        return OperationResult.ok(generated);
    }

    /**
     * Edita el contenido de una pregunta existente (usado por la API REST,
     * PUT). Reglas de negocio:
     *  - Solo el autor de la pregunta puede editarla.
     *  - Solo se editan preguntas en estado BORRADOR (una pregunta enviada a
     *    revision no puede cambiar mientras se revisa).
     *  - Los nuevos datos pasan por el MISMO pipeline de validacion
     *    estructural del microkernel que se usa al crear (HU03).
     * Se conservan id, estado, autor, fecha de creacion y revisores.
     */
    public OperationResult actualizarPregunta(String questionId, QuestionRequest request) {
        Optional<Question> found = questionRepository.findById(questionId);
        if (found.isEmpty()) {
            return OperationResult.fail("No existe una pregunta con el ID: " + questionId);
        }
        Question existing = found.get();
        if (request.getAuthorLogin() == null || !existing.getAuthorLogin().equals(request.getAuthorLogin())) {
            return OperationResult.fail("Solo el autor de la pregunta puede editarla.");
        }
        if (existing.getState() != QuestionState.BORRADOR) {
            return OperationResult.fail("Solo se puede editar una pregunta en estado Borrador.");
        }

        String type = request.getType() != null && !request.getType().isBlank()
                ? request.getType() : existing.getType();
        Question validated;
        try {
            validated = microkernel.executePlugin(type, request);
        } catch (IllegalArgumentException ex) {
            return OperationResult.fail(ex.getMessage());
        }
        if (validated == null) {
            return OperationResult.fail("La pregunta no supero la validacion estructural: "
                    + microkernel.getLastError());
        }
        // El plugin genera una Question nueva (con otro id); se descarta ese
        // id temporal del banco en memoria del nucleo y se conserva el original.
        microkernel.getQuestions().remove(validated.getId());

        existing.setType(validated.getType());
        existing.setContext(validated.getContext());
        existing.setDirectQuestion(validated.getDirectQuestion());
        existing.setDistractors(validated.getDistractors());
        existing.setCorrectAnswer(validated.getCorrectAnswer());
        existing.setJustification(validated.getJustification());
        existing.setBibliography(validated.getBibliography());
        existing.setCompetence(validated.getCompetence());
        existing.setTopic(validated.getTopic());
        existing.setSubtopic(validated.getSubtopic());
        existing.setDifficultyLevel(validated.getDifficultyLevel());

        if (!questionRepository.update(existing)) {
            return OperationResult.fail("No fue posible actualizar la pregunta en la base de datos.");
        }
        microkernel.getQuestions().put(existing.getId(), existing);
        subject.notificarObservers();
        return OperationResult.ok(existing);
    }

    /**
     * Eliminacion LOGICA de una pregunta (usado por la API REST, DELETE): la
     * pregunta pasa al estado ELIMINADA, que ya existia en el ciclo de vida
     * del dominio, en lugar de borrarse fisicamente. Asi se conserva la
     * trazabilidad y no se modifica el contrato IQuestionRepository.
     */
    public OperationResult eliminarPregunta(String questionId) {
        Optional<Question> found = questionRepository.findById(questionId);
        if (found.isEmpty()) {
            return OperationResult.fail("No existe una pregunta con el ID: " + questionId);
        }
        Question question = found.get();
        if (question.getState() == QuestionState.ELIMINADA) {
            return OperationResult.fail("La pregunta ya se encuentra eliminada.");
        }
        question.changeState(QuestionState.ELIMINADA);
        if (!questionRepository.update(question)) {
            return OperationResult.fail("No fue posible eliminar la pregunta en la base de datos.");
        }
        microkernel.getQuestions().remove(questionId);
        subject.notificarObservers();
        return OperationResult.ok(question);
    }

    /**
     * HU02: cambia el estado de una pregunta propia de "borrador" a
     * "Pendiente de revision" (unica transicion habilitada al autor).
     */
    public OperationResult marcarPendienteDeRevision(String questionId, String authorLogin) {
        Optional<Question> found = questionRepository.findById(questionId);
        if (found.isEmpty()) {
            return OperationResult.fail("No existe una pregunta con el ID: " + questionId);
        }
        Question question = found.get();
        if (!question.getAuthorLogin().equals(authorLogin)) {
            return OperationResult.fail("Solo el autor de la pregunta puede cambiar su estado.");
        }
        if (question.getState() != QuestionState.BORRADOR) {
            return OperationResult.fail("Solo se puede enviar a revision una pregunta en estado Borrador.");
        }

        question.changeState(QuestionState.PENDIENTE_REVISION);
        boolean updated = questionRepository.update(question);
        if (!updated) {
            return OperationResult.fail("No fue posible actualizar el estado en la base de datos.");
        }
        subject.notificarObservers();
        return OperationResult.ok(question);
    }

    /** HU03: listado paginado y filtrado de las preguntas de un autor. */
    public PagedResult<Question> listarMisPreguntas(QuestionFilterCriteria criteria) {
        return questionRepository.search(criteria);
    }

    /**
     * @return el detalle del ultimo error de base de datos ocurrido en la
     * ultima consulta (por ejemplo al listar preguntas), o {@code null} si
     * no hubo error. Se usa en la capa de presentacion para mostrar un
     * mensaje visible en vez de dejar la tabla vacia sin explicacion.
     */
    public String getUltimoErrorDeBaseDeDatos() {
        return questionRepository.getLastError();
    }

    public List<Question> listarPendientesDeRevision() {
        return questionRepository.findByState(QuestionState.PENDIENTE_REVISION);
    }

    public Optional<Question> buscarPorId(String id) {
        return questionRepository.findById(id);
    }

    /**
     * HU04: el administrador asigna al menos un revisor a una pregunta en
     * estado "Pendiente de revision". Al asignar, el sistema notifica por
     * correo a cada revisor y la pregunta pasa a "En revision".
     */
    public OperationResult asignarRevisores(String questionId, List<String> reviewerLogins) {
        if (reviewerLogins == null || reviewerLogins.isEmpty()) {
            return OperationResult.fail("Debe seleccionar al menos un revisor.");
        }
        Optional<Question> found = questionRepository.findById(questionId);
        if (found.isEmpty()) {
            return OperationResult.fail("No existe una pregunta con el ID: " + questionId);
        }
        Question question = found.get();
        if (question.getState() != QuestionState.PENDIENTE_REVISION) {
            return OperationResult.fail(
                    "Solo se pueden asignar revisores a preguntas en estado Pendiente de revision.");
        }

        for (String reviewerLogin : reviewerLogins) {
            question.assignReviewer(reviewerLogin);
        }
        question.changeState(QuestionState.EN_REVISION);
        boolean updated = questionRepository.update(question);
        if (!updated) {
            return OperationResult.fail("No fue posible actualizar la pregunta en la base de datos.");
        }

        notifyReviewers(question, reviewerLogins);

        subject.notificarObservers();
        return OperationResult.ok(question);
    }

    private void notifyReviewers(Question question, List<String> reviewerLogins) {
        for (String reviewerLogin : reviewerLogins) {
            userService.listReviewers().stream()
                    .filter(u -> u.getLogin().equals(reviewerLogin))
                    .findFirst()
                    .ifPresent((User reviewer) -> emailService.sendReviewAssignmentNotification(
                            reviewer.getEmail(), reviewer.getFullName(),
                            question.getId(), question.getDirectQuestion()));
        }
    }

    public void agregarObserver(Observer observer) {
        subject.agregarObserver(observer);
    }

    public void eliminarObserver(Observer observer) {
        subject.eliminarObserver(observer);
    }
}
