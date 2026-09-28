package co.unicauca.saberpro.service;

import co.unicauca.saberpro.domain.QuestionState;

/**
 * Criterios de busqueda/paginacion usados por HU03 ("aplicar paginacion y
 * filtros para hacer funcional y usable la busqueda y listado de
 * preguntas").
 */
public class QuestionFilterCriteria {

    private String authorLogin;       // null => no filtra por autor (uso admin)
    private QuestionState state;      // null => cualquier estado
    private String topic;             // null/blank => cualquier tema
    private String keyword;           // busca en pregunta directa / contexto
    private int page = 1;             // 1-based
    private int pageSize = 5;

    public String getAuthorLogin() { return authorLogin; }
    public QuestionFilterCriteria setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; return this; }

    public QuestionState getState() { return state; }
    public QuestionFilterCriteria setState(QuestionState state) { this.state = state; return this; }

    public String getTopic() { return topic; }
    public QuestionFilterCriteria setTopic(String topic) { this.topic = topic; return this; }

    public String getKeyword() { return keyword; }
    public QuestionFilterCriteria setKeyword(String keyword) { this.keyword = keyword; return this; }

    public int getPage() { return page; }
    public QuestionFilterCriteria setPage(int page) { this.page = Math.max(1, page); return this; }

    public int getPageSize() { return pageSize; }
    public QuestionFilterCriteria setPageSize(int pageSize) { this.pageSize = Math.max(1, pageSize); return this; }
}
