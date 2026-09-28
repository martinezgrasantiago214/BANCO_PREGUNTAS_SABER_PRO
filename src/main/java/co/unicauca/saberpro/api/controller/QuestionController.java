package co.unicauca.saberpro.api.controller;

import co.unicauca.saberpro.api.dto.AssignReviewersDTO;
import co.unicauca.saberpro.api.dto.QuestionDTO;
import co.unicauca.saberpro.api.dto.QuestionResponseDTO;
import co.unicauca.saberpro.api.dto.SubmitReviewDTO;
import co.unicauca.saberpro.api.exception.BusinessException;
import co.unicauca.saberpro.api.exception.ResourceNotFoundException;
import co.unicauca.saberpro.domain.DifficultyLevel;
import co.unicauca.saberpro.domain.Question;
import co.unicauca.saberpro.domain.QuestionRequest;
import co.unicauca.saberpro.domain.QuestionState;
import co.unicauca.saberpro.microkernel.plugins.MultipleChoiceQuestionPlugin;
import co.unicauca.saberpro.service.OperationResult;
import co.unicauca.saberpro.service.PagedResult;
import co.unicauca.saberpro.service.QuestionFilterCriteria;
import co.unicauca.saberpro.service.QuestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

/**
 * Controlador REST del recurso "preguntas" (/api/questions).
 *
 * Es solo la capa de presentacion HTTP: toda la logica (pipeline de
 * validacion del microkernel, reglas de estado, persistencia) sigue en
 * QuestionService, igual que para la interfaz Swing.
 *
 *   GET    /api/questions                      Listar (filtros + paginacion, HU03)
 *   GET    /api/questions/{id}                 Consultar una pregunta
 *   POST   /api/questions                      Crear (microkernel + pipeline, HU01)
 *   PUT    /api/questions/{id}                 Editar (solo autor y en Borrador)
 *   DELETE /api/questions/{id}                 Eliminar (logica: estado ELIMINADA)
 *   PATCH  /api/questions/{id}/submit-review   Enviar a revision (HU02)
 *   PATCH  /api/questions/{id}/reviewers       Asignar revisores (HU04)
 *
 * Los metodos son synchronized porque el repositorio SQLite existente
 * comparte una unica conexion JDBC, y Tomcat atiende peticiones en varios
 * hilos; asi se serializa el acceso sin modificar la capa de acceso a datos.
 */
@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public synchronized PagedResult<QuestionResponseDTO> list(
            @RequestParam(name = "author", required = false) String author,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "topic", required = false) String topic,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin(author)
                .setState(parseState(state))
                .setTopic(topic)
                .setKeyword(keyword)
                .setPage(page)
                .setPageSize(size);

        PagedResult<Question> result = questionService.listarMisPreguntas(criteria);
        String dbError = questionService.getUltimoErrorDeBaseDeDatos();
        if (dbError != null) {
            throw new IllegalStateException(dbError);
        }
        List<QuestionResponseDTO> items = result.getItems().stream().map(QuestionResponseDTO::from).toList();
        return new PagedResult<>(items, result.getPage(), result.getPageSize(), result.getTotalItems());
    }

    @GetMapping("/{id}")
    public synchronized QuestionResponseDTO getById(@PathVariable("id") String id) {
        return QuestionResponseDTO.from(findOrThrow(id));
    }

    @PostMapping
    public synchronized ResponseEntity<QuestionResponseDTO> create(@RequestBody QuestionDTO body) {
        OperationResult result = questionService.crearPregunta(toDomain(body));
        Question created = unwrap(result);
        return ResponseEntity
                .created(URI.create("/api/questions/" + created.getId()))
                .body(QuestionResponseDTO.from(created));
    }

    @PutMapping("/{id}")
    public synchronized QuestionResponseDTO update(@PathVariable("id") String id, @RequestBody QuestionDTO body) {
        findOrThrow(id);
        OperationResult result = questionService.actualizarPregunta(id, toDomain(body));
        return QuestionResponseDTO.from(unwrap(result));
    }

    @DeleteMapping("/{id}")
    public synchronized QuestionResponseDTO delete(@PathVariable("id") String id) {
        findOrThrow(id);
        OperationResult result = questionService.eliminarPregunta(id);
        return QuestionResponseDTO.from(unwrap(result));
    }

    @PatchMapping("/{id}/submit-review")
    public synchronized QuestionResponseDTO submitReview(@PathVariable("id") String id,
                                                         @RequestBody SubmitReviewDTO body) {
        findOrThrow(id);
        OperationResult result = questionService.marcarPendienteDeRevision(id, body.getAuthorLogin());
        return QuestionResponseDTO.from(unwrap(result));
    }

    @PatchMapping("/{id}/reviewers")
    public synchronized QuestionResponseDTO assignReviewers(@PathVariable("id") String id,
                                                            @RequestBody AssignReviewersDTO body) {
        findOrThrow(id);
        OperationResult result = questionService.asignarRevisores(id, body.getReviewers());
        return QuestionResponseDTO.from(unwrap(result));
    }

    // ----------------------------------------------------------------- helpers

    private Question findOrThrow(String id) {
        return questionService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una pregunta con el ID: " + id));
    }

    private Question unwrap(OperationResult result) {
        if (!result.isSuccess()) {
            throw new BusinessException(result.getErrors());
        }
        return (Question) result.getData();
    }

    private QuestionRequest toDomain(QuestionDTO dto) {
        String type = (dto.getType() == null || dto.getType().isBlank())
                ? MultipleChoiceQuestionPlugin.TYPE : dto.getType().trim();
        return new QuestionRequest(
                type,
                dto.getContext(),
                dto.getDirectQuestion(),
                dto.getDistractors(),
                dto.getCorrectAnswer(),
                dto.getJustification(),
                dto.getBibliography(),
                dto.getCompetence(),
                dto.getTopic(),
                dto.getSubtopic(),
                parseDifficulty(dto.getDifficultyLevel()),
                dto.getAuthorLogin());
    }

    private DifficultyLevel parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null; // el ClassificationFilter del pipeline reportara el error
        }
        try {
            return DifficultyLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("difficultyLevel invalido: '" + value
                    + "'. Valores permitidos: " + Arrays.toString(DifficultyLevel.values()));
        }
    }

    private QuestionState parseState(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return QuestionState.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("state invalido: '" + value + "'. Valores permitidos: "
                    + Arrays.stream(QuestionState.values()).map(Enum::name).toList());
        }
    }
}
