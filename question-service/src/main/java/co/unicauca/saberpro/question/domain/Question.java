package co.unicauca.saberpro.question.domain;

import co.unicauca.saberpro.common.contract.DifficultyLevel;
import co.unicauca.saberpro.common.contract.QuestionStatus;
import co.unicauca.saberpro.question.domain.state.QuestionState;
import co.unicauca.saberpro.question.domain.state.QuestionStates;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio: pregunta de seleccion multiple con unica respuesta,
 * disenada de acuerdo con el Diseno Centrado en Evidencia (HU01).
 *
 * Patron Builder (GoF, creacional): una pregunta tiene 12 datos; en lugar de
 * un constructor con 12 parametros del mismo tipo (facil de invertir sin que
 * el compilador lo note) se construye paso a paso con {@link #builder()}.
 *
 * Patron State (GoF, comportamiento): el ciclo de vida (Borrador ->
 * Pendiente de revision -> Aprobada/Rechazada, o Eliminada) lo decide el
 * objeto {@link QuestionState} actual; la pregunta solo delega en el.
 */
public class Question {

    private final String id;
    private final String type;
    private final String context;
    private final String directQuestion;
    private final List<String> distractors;
    private final String correctAnswer;
    private final String justification;
    private final String bibliography;
    private final String competence;
    private final String topic;
    private final String subtopic;
    private final DifficultyLevel difficultyLevel;
    private final String authorLogin;
    private final LocalDateTime createdAt;
    private QuestionState state;

    private Question(Builder builder) {
        this.id = builder.id != null ? builder.id : UUID.randomUUID().toString();
        this.type = builder.type;
        this.context = builder.context;
        this.directQuestion = builder.directQuestion;
        this.distractors = List.copyOf(builder.distractors);
        this.correctAnswer = builder.correctAnswer;
        this.justification = builder.justification;
        this.bibliography = builder.bibliography;
        this.competence = builder.competence;
        this.topic = builder.topic;
        this.subtopic = builder.subtopic;
        this.difficultyLevel = builder.difficultyLevel;
        this.authorLogin = builder.authorLogin;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.state = QuestionStates.from(builder.status);
    }

    public static Builder builder() {
        return new Builder();
    }

    // ---- Ciclo de vida (patron State): la pregunta delega en su estado ----

    /** HU02: Borrador -> Pendiente de revision. */
    public void submitForReview() {
        state = state.submitForReview();
    }

    /** HU05: Pendiente de revision -> Aprobada. */
    public void approve() {
        state = state.approve();
    }

    /** HU05: Pendiente de revision -> Rechazada. */
    public void reject() {
        state = state.reject();
    }

    /** Eliminacion logica: solo un Borrador se puede eliminar. */
    public void delete() {
        state = state.delete();
    }

    /** HU03: solo se edita una pregunta en Borrador. */
    public boolean isEditable() {
        return state.isEditable();
    }

    public QuestionStatus getStatus() {
        return state.status();
    }

    public boolean isAuthoredBy(String login) {
        return authorLogin != null && authorLogin.equals(login);
    }

    // ---- Getters ----

    public String getId() { return id; }
    public String getType() { return type; }
    public String getContext() { return context; }
    public String getDirectQuestion() { return directQuestion; }
    public List<String> getDistractors() { return distractors; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getJustification() { return justification; }
    public String getBibliography() { return bibliography; }
    public String getCompetence() { return competence; }
    public String getTopic() { return topic; }
    public String getSubtopic() { return subtopic; }
    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public String getAuthorLogin() { return authorLogin; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "[" + getStatus().getLabel() + "] " + directQuestion + " (id=" + id + ")";
    }

    /**
     * Builder de {@link Question}. Si no se indica, el id se genera, la fecha
     * de creacion es "ahora" y el estado inicial es Borrador (HU02, criterio 1).
     */
    public static final class Builder {

        private String id;
        private String type;
        private String context;
        private String directQuestion;
        private List<String> distractors = new ArrayList<>();
        private String correctAnswer;
        private String justification;
        private String bibliography;
        private String competence;
        private String topic;
        private String subtopic;
        private DifficultyLevel difficultyLevel;
        private String authorLogin;
        private LocalDateTime createdAt;
        private QuestionStatus status = QuestionStatus.BORRADOR;

        private Builder() {
        }

        public Builder id(String id) { this.id = id; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder context(String context) { this.context = context; return this; }
        public Builder directQuestion(String directQuestion) { this.directQuestion = directQuestion; return this; }

        public Builder distractors(List<String> distractors) {
            this.distractors = distractors != null ? new ArrayList<>(distractors) : new ArrayList<>();
            return this;
        }

        public Builder correctAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; return this; }
        public Builder justification(String justification) { this.justification = justification; return this; }
        public Builder bibliography(String bibliography) { this.bibliography = bibliography; return this; }
        public Builder competence(String competence) { this.competence = competence; return this; }
        public Builder topic(String topic) { this.topic = topic; return this; }
        public Builder subtopic(String subtopic) { this.subtopic = subtopic; return this; }
        public Builder difficultyLevel(DifficultyLevel level) { this.difficultyLevel = level; return this; }
        public Builder authorLogin(String authorLogin) { this.authorLogin = authorLogin; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        /** Usado al reconstruir una pregunta guardada; por defecto el estado es Borrador. */
        public Builder status(QuestionStatus status) {
            this.status = Objects.requireNonNull(status, "status");
            return this;
        }

        /** Copia en el builder todos los datos capturados por el autor. */
        public Builder fromRequest(QuestionRequest request) {
            return context(request.getContext())
                    .directQuestion(request.getDirectQuestion())
                    .distractors(request.getDistractors())
                    .correctAnswer(request.getCorrectAnswer())
                    .justification(request.getJustification())
                    .bibliography(request.getBibliography())
                    .competence(request.getCompetence())
                    .topic(request.getTopic())
                    .subtopic(request.getSubtopic())
                    .difficultyLevel(request.getDifficultyLevel())
                    .authorLogin(request.getAuthorLogin());
        }

        public Question build() {
            if (type == null || type.isBlank()) {
                throw new IllegalStateException("Toda pregunta debe tener un tipo (p. ej. MULTIPLE_CHOICE).");
            }
            return new Question(this);
        }
    }
}
