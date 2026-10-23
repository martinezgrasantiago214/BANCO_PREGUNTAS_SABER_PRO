package co.unicauca.saberpro.common.web;

/**
 * Marca un caso de uso que todavia no esta implementado y dice que issue de
 * Jira lo resuelve. Se responde con 501 Not Implemented, de modo que el
 * frontend y Postman muestran claramente que falta y en que issue.
 */
public class PendingIssueException extends RuntimeException {

    private final String issueKey;

    public PendingIssueException(String issueKey, String description) {
        super("Pendiente de implementar (" + issueKey + "): " + description);
        this.issueKey = issueKey;
    }

    public String getIssueKey() {
        return issueKey;
    }
}
