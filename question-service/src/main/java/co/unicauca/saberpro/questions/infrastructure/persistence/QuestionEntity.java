package co.unicauca.saberpro.questions.infrastructure.persistence;

import co.unicauca.saberpro.questions.domain.lifecycle.QuestionStateType;
import co.unicauca.saberpro.questions.domain.model.DifficultyLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Modelo de persistencia de la pregunta (independiente del agregado de dominio). */
@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_question_author", columnList = "authorLogin"),
        @Index(name = "idx_question_state", columnList = "state")
})
public class QuestionEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 60)
    private String authorLogin;

    @Column(nullable = false, length = 4000)
    private String context;

    @Column(nullable = false, length = 2000)
    private String statement;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderBy("position ASC")
    private List<OptionEmbeddable> options = new ArrayList<>();

    @Column(nullable = false, length = 4000)
    private String justification;

    @Column(nullable = false, length = 2000)
    private String bibliography;

    @Column(nullable = false, length = 200)
    private String competence;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(nullable = false, length = 200)
    private String subtopic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DifficultyLevel difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuestionStateType state;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /** Bloqueo optimista: evita que dos instancias pisen el mismo cambio de estado. */
    @Version
    private Long version;

    protected QuestionEntity() {
    }

    // getters y setters usados por el mapeador
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }
    public List<OptionEmbeddable> getOptions() { return options; }
    public void setOptions(List<OptionEmbeddable> options) { this.options = options; }
    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }
    public String getBibliography() { return bibliography; }
    public void setBibliography(String bibliography) { this.bibliography = bibliography; }
    public String getCompetence() { return competence; }
    public void setCompetence(String competence) { this.competence = competence; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getSubtopic() { return subtopic; }
    public void setSubtopic(String subtopic) { this.subtopic = subtopic; }
    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; }
    public QuestionStateType getState() { return state; }
    public void setState(QuestionStateType state) { this.state = state; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
