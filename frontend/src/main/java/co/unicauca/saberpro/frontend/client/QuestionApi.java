package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.frontend.model.Models.PageView;
import co.unicauca.saberpro.frontend.model.Models.QuestionForm;
import co.unicauca.saberpro.frontend.model.Models.QuestionView;

/** Operaciones del question-service (via gateway). */
public interface QuestionApi {

    QuestionView create(QuestionForm form);

    QuestionView update(String id, QuestionForm form);

    QuestionView findById(String id);

    PageView<QuestionView> search(String author, QuestionStatus status, String topic, String keyword,
                                  int page, int size);

    QuestionView submitForReview(String id, String authorLogin);
}
