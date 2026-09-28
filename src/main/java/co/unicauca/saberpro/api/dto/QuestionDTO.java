package co.unicauca.saberpro.api.dto;

import java.util.List;

/**
 * Cuerpo JSON de entrada para crear (POST) o editar (PUT) una pregunta.
 * Es un DTO propio de la API: la entidad de dominio Question no se
 * "contamina" con detalles de HTTP/JSON.
 */
public class QuestionDTO {

    private String type;             // MULTIPLE_CHOICE (por defecto), CASO, MULTIMEDIA
    private String context;
    private String directQuestion;
    private List<String> distractors;
    private String correctAnswer;
    private String justification;
    private String bibliography;
    private String competence;
    private String topic;
    private String subtopic;
    private String difficultyLevel;  // BAJO, MEDIO, ALTO
    private String authorLogin;

    public QuestionDTO() {
    }

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

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
}
