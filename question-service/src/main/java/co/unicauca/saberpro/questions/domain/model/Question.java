package co.unicauca.saberpro.questions.domain.model;

import co.unicauca.saberpro.questions.domain.exception.QuestionDomainException;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionState;
import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Agregado raiz del contexto Gestion de Preguntas: pregunta de seleccion
 * multiple con unica respuesta disenada con Diseno Centrado en Evidencia.
 *
 * - Patron Builder (GoF, creacional): la pregunta tiene 14 atributos; el
 *   Builder evita un constructor telescopico y deja la construccion legible
 *   tanto en el plugin (creacion) como en el mapeador de persistencia
 *   (rehidratacion).
 * - Patron State (GoF, comportamiento): las transiciones del ciclo de vida
 *   se delegan en el objeto {@link QuestionState} actual.
 */
public class Question {

    private final String id;
    private final String type;
    private final String authorLogin;
    private final Instant createdAt;
    private String context;
    private String statement;
    private List<QuestionOption> options;
    private String justification;
    private String bibliography;
    private String competence;
    private String topic;
    private String subtopic;
    private DifficultyLevel difficulty;
    private Instant updatedAt;
    private QuestionState state;
    /** Version para control de concurrencia optimista (null = aun no persistida). */
    private final Long version;

    private Question(Builder b) {
        this.id = Objects.requireNonNull(b.id, "id");
        this.type = Objects.requireNonNull(b.type, "type");
        this.authorLogin = Objects.requireNonNull(b.authorLogin, "authorLogin");
        this.createdAt = b.createdAt == null ? Instant.now() : b.createdAt;
        this.updatedAt = b.updatedAt == null ? this.createdAt : b.updatedAt;
        this.context = b.context;
        this.statement = b.statement;
        this.options = List.copyOf(b.options);
        this.justification = b.justification;
        this.bibliography = b.bibliography;
        this.competence = b.competence;
        this.topic = b.topic;
        this.subtopic = b.subtopic;
        this.difficulty = b.difficulty;
        this.state = b.state.toState();
        this.version = b.version;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------ ciclo de vida (State)

    /** HU02: Borrador -> Pendiente de revision. */
    public void submitForReview() {
        state = state.submitForReview();
        touch();
    }

    /** HU04: al asignar revisores la pregunta pasa a En revision. */
    public void startReview() {
        state = state.startReview();
        touch();
    }

    /** HU05: resultado de la revision. */
    public void approve() {
        state = state.approve();
        touch();
    }

    public void reject() {
        state = state.reject();
        touch();
    }

    /**
     * Edita el contenido. Solo se permite en estados editables; si estaba
     * Rechazada vuelve a Borrador.
     */
    public void edit(QuestionData data, List<QuestionOption> newOptions) {
        QuestionState next = state.edit();
        this.context = data.context();
        this.statement = data.statement();
        this.options = List.copyOf(newOptions);
        this.justification = data.justification();
        this.bibliography = data.bibliography();
        this.competence = data.competence();
        this.topic = data.topic();
        this.subtopic = data.subtopic();
        this.difficulty = data.difficulty();
        this.state = next;
        touch();
    }

    public void ensureDeletable() {
        if (!state.isDeletable()) {
            throw new QuestionDomainException(QuestionDomainException.Kind.INVALID_TRANSITION,
                    "Solo se pueden eliminar preguntas en Borrador o Rechazada.");
        }
    }

    public boolean isOwnedBy(String login) {
        return authorLogin.equals(login);
    }

    public boolean isEditable() {
        return state.isEditable();
    }

    public QuestionStateType getStateType() {
        return state.type();
    }

    public String getCorrectAnswer() {
        return options.stream().filter(QuestionOption::correct).map(QuestionOption::text).findFirst().orElse(null);
    }

    public List<String> getOptionTexts() {
        return options.stream().map(QuestionOption::text).toList();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    // ------------------------------------------------------------------ getters

    public String getId() { return id; }
    public String getType() { return type; }
    public String getAuthorLogin() { return authorLogin; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getContext() { return context; }
    public String getStatement() { return statement; }
    public List<QuestionOption> getOptions() { return options; }
    public String getJustification() { return justification; }
    public String getBibliography() { return bibliography; }
    public String getCompetence() { return competence; }
    public String getTopic() { return topic; }
    public String getSubtopic() { return subtopic; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public Long getVersion() { return version; }

    /** Builder (GoF): construccion paso a paso con interfaz fluida. */
    public static final class Builder {
        private String id;
        private String type;
        private String authorLogin;
        private Instant createdAt;
        private Instant updatedAt;
        private String context;
        private String statement;
        private final List<QuestionOption> options = new ArrayList<>();
        private String justification;
        private String bibliography;
        private String competence;
        private String topic;
        private String subtopic;
        private DifficultyLevel difficulty;
        private QuestionStateType state = QuestionStateType.BORRADOR;
        private Long version;

        private Builder() {
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder authorLogin(String authorLogin) { this.authorLogin = authorLogin; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder context(String context) { this.context = context; return this; }
        public Builder statement(String statement) { this.statement = statement; return this; }
        public Builder justification(String justification) { this.justification = justification; return this; }
        public Builder bibliography(String bibliography) { this.bibliography = bibliography; return this; }
        public Builder competence(String competence) { this.competence = competence; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder subtopic(String subtopic) { this.subtopic = subtopic; return this; }
        public Builder difficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; return this; }
        public Builder state(QuestionStateType state) { this.state = state; return this; }
        public Builder version(Long version) { this.version = version; return this; }

        public Builder option(String text, boolean correct) {
            this.options.add(new QuestionOption(text, correct, options.size() + 1));
            return this;
        }

        public Builder options(List<QuestionOption> options) {
            this.options.clear();
            this.options.addAll(options);
            return this;
        }

        public Question build() {
            long correctCount = options.stream().filter(QuestionOption::correct).count();
            if (correctCount > 1) {
                throw new IllegalStateException("Una pregunta de única respuesta no puede tener varias opciones correctas.");
            }
            return new Question(this);
        }
    }
}
