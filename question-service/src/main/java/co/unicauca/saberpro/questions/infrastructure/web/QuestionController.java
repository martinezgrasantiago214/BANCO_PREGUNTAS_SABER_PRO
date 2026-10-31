package co.unicauca.saberpro.questions.infrastructure.web;

import co.unicauca.saberpro.questions.application.QuestionSearchCriteria;
import co.unicauca.saberpro.questions.application.QuestionService;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import co.unicauca.saberpro.questions.infrastructure.web.dto.CatalogItem;
import co.unicauca.saberpro.questions.infrastructure.web.dto.PageResponse;
import co.unicauca.saberpro.questions.infrastructure.web.dto.QuestionRequest;
import co.unicauca.saberpro.questions.infrastructure.web.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * API REST del contexto de preguntas. La identidad del usuario llega en el
 * encabezado X-User-Login, que el frontend envia en cada solicitud.
 */
@RestController
@RequestMapping("/api/questions")
@Tag(name = "Preguntas", description = "HU01 crear, HU02 enviar a revisión, HU03 listar/editar")
public class QuestionController {

    static final String USER_HEADER = "X-User-Login";
    private final QuestionService service;

    public QuestionController(QuestionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU01 - Crea una pregunta (queda en Borrador) aplicando la validación estructural")
    public QuestionResponse create(@RequestHeader(USER_HEADER) String user, @RequestBody QuestionRequest body) {
        return QuestionResponse.from(service.create(body.toData(user)));
    }

    @GetMapping
    @Operation(summary = "HU03 - Lista preguntas con paginación y filtros (author=login para 'mis preguntas')")
    public PageResponse<QuestionResponse> search(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) QuestionStateType state,
            @RequestParam(required = false) String competence,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) DifficultyLevel difficulty,
            @RequestParam(required = false) String keyword,
            @Parameter(description = "Página, inicia en 1") @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        QuestionSearchCriteria criteria = new QuestionSearchCriteria(author, state, competence, topic,
                difficulty, keyword, page, size);
        return PageResponse.from(service.search(criteria).map(QuestionResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta el detalle completo de una pregunta")
    public QuestionResponse get(@PathVariable String id) {
        return QuestionResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "HU03 - Edita una pregunta propia en Borrador o Rechazada")
    public QuestionResponse update(@RequestHeader(USER_HEADER) String user, @PathVariable String id,
                                   @RequestBody QuestionRequest body) {
        return QuestionResponse.from(service.update(id, body.toData(user), user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina una pregunta propia en Borrador o Rechazada")
    public void delete(@RequestHeader(USER_HEADER) String user, @PathVariable String id) {
        service.delete(id, user);
    }

    @PostMapping("/{id}/submit-for-review")
    @Operation(summary = "HU02 - Cambia el estado de Borrador a Pendiente de revisión y publica el evento")
    public QuestionResponse submitForReview(@RequestHeader(USER_HEADER) String user, @PathVariable String id) {
        return QuestionResponse.from(service.submitForReview(id, user));
    }

    @GetMapping("/catalog/states")
    @Operation(summary = "HU02 - Estados del ciclo de vida con su color")
    public List<CatalogItem> states() {
        return Arrays.stream(QuestionStateType.values())
                .map(s -> new CatalogItem(s.name(), s.getLabel(), s.getColorHex())).toList();
    }

    @GetMapping("/catalog/types")
    @Operation(summary = "Tipos de pregunta registrados en el microkernel (plugins)")
    public List<CatalogItem> types() {
        return service.getRegistry().plugins().stream()
                .map(p -> new CatalogItem(p.type(), p.displayName(), null)).toList();
    }
}
