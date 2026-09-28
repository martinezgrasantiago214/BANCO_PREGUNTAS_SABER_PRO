package co.unicauca.saberpro.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio: pregunta de seleccion multiple con unica respuesta,
 * disenada de acuerdo con los principios del Diseno Centrado en Evidencia
 * (HU01) para alimentar el banco de preguntas Saber Pro.
 */
public class Question {

    private String id;
    private String type;            // MULTIPLE_CHOICE, CASO, MULTIMEDIA (plugin que la genero)
    private String context;         // Contexto
    private String directQuestion;  // Pregunta directa
    private List<String> distractors = new ArrayList<>(); // Cuatro distractores (incluye la correcta)
    private String correctAnswer;   // Respuesta correcta
    private String justification;   // Justificacion de la respuesta
    private String bibliography;    // Bibliografia
    private String competence;      // Competencia
    private String topic;           // Tema
    private String subtopic;        // Subtema
    private DifficultyLevel difficultyLevel; // Nivel de dificultad
    private QuestionState state = QuestionState.BORRADOR;
    private String authorLogin;
    private LocalDateTime createdAt = LocalDateTime.now();
    private List<String> assignedReviewers = new ArrayList<>();

    public Question() {
    }

    public Question(String id, String type, String context, String directQuestion,
                     List<String> distractors, String correctAnswer, String justification,
                     String bibliography, String competence, String topic, String subtopic,
                     DifficultyLevel difficultyLevel, String authorLogin) {
        this.id = id;
        this.type = type;
        this.context = context;
        this.directQuestion = directQuestion;
        this.distractors = distractors != null ? distractors : new ArrayList<>();
        this.correctAnswer = correctAnswer;
        this.justification = justification;
        this.bibliography = bibliography;
        this.competence = competence;
        this.topic = topic;
        this.subtopic = subtopic;
        this.difficultyLevel = difficultyLevel;
        this.authorLogin = authorLogin;
    }

    public void changeState(QuestionState newState) {
        this.state = newState;
    }

    public void assignReviewer(String reviewerLogin) {
        if (!assignedReviewers.contains(reviewerLogin)) {
            assignedReviewers.add(reviewerLogin);
        }
    }

    // Getters / Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }

    public String getDirectQuestion() { return directQuestion; }
    public void setDirectQuestion(String directQuestion) { this.directQuestion = directQuestion; }

    public List<String> getDistractors() { return distractors; }
    public void setDistractors(List<String> distractors) { this.distractors = distractors; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

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

    public DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(DifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public QuestionState getState() { return state; }
    public void setState(QuestionState state) { this.state = state; }

    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getAssignedReviewers() { return assignedReviewers; }
    public void setAssignedReviewers(List<String> assignedReviewers) { this.assignedReviewers = assignedReviewers; }

    @Override
    public String toString() {
        return "[" + state.getLabel() + "] " + directQuestion + " (id=" + id + ")";
    }
}
