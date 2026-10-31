package co.unicauca.saberpro.question.web;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.access.QuestionFilterCriteria;
import co.unicauca.saberpro.question.application.QuestionApplicationService;
import co.unicauca.saberpro.question.web.QuestionDtos.PageResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionInput;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.QuestionTypeResponse;
import co.unicauca.saberpro.question.web.QuestionDtos.SubmitReviewInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** API REST de preguntas. Los casos de uso pendientes responden 501 indicando su issue de Jira. */
@RestController
@RequestMapping("/api/questions")
@Tag(name = "Preguntas")
public class QuestionController {

    private final QuestionApplicationService service;

    public QuestionController(QuestionApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "HU01: crea una pregunta (queda en Borrador) [PSP-16/17/18]")
    public QuestionResponse create(@RequestBody QuestionInput input) {
        return QuestionResponse.from(service.create(input.toRequest()));
    }

    @GetMapping
    @Operation(summary = "HU03: lista preguntas con filtros y paginacion [PSP-24..27]")
    public PageResponse<QuestionResponse> search(@RequestParam(required = false) String author,
                                                 @RequestParam(required = false) QuestionStatus status,
                                                 @RequestParam(required = false) String topic,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        QuestionFilterCriteria criteria = new QuestionFilterCriteria()
                .setAuthorLogin(author).setStatus(status).setTopic(topic).setKeyword(keyword)
                .setPage(page).setPageSize(size);
        return PageResponse.from(service.search(criteria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta una pregunta por id")
    public QuestionResponse findById(@PathVariable String id) {
        return QuestionResponse.from(service.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "HU03: edita una pregunta en Borrador [PSP-28]")
    public QuestionResponse update(@PathVariable String id, @RequestBody QuestionInput input) {
        return QuestionResponse.from(service.update(id, input.toRequest()));
    }

    @PatchMapping("/{id}/submit-review")
    @Operation(summary = "HU02: Borrador -> Pendiente de revision [PSP-21]")
    public QuestionResponse submitForReview(@PathVariable String id, @RequestBody SubmitReviewInput input) {
        return QuestionResponse.from(service.submitForReview(id, input.authorLogin()));
    }

    @GetMapping("/types")
    @Operation(summary = "Tipos de pregunta disponibles (fabricas cargadas por reflexion)")
    public List<QuestionTypeResponse> types() {
        return service.availableTypes().stream().map(QuestionTypeResponse::from).toList();
    }
}
