package co.unicauca.saberpro.domain;

import java.util.List;

/**
 * DTO que transporta los datos capturados en la capa de presentacion
 * (formulario de creacion de pregunta, HU01) hacia el microkernel y sus
 * plugins, pasando por el pipeline de validacion (Tuberias y Filtros).
 */
public class QuestionRequest {

    private String type;
    private String context;
    private String directQuestion;
    private List<String> distractors;
    private String correctAnswer;
    private String justification;
    private String bibliography;
    private String competence;
    private String topic;
    private String subtopic;
    private DifficultyLevel difficultyLevel;
    private String authorLogin;

    public QuestionRequest(String type, String context, String directQuestion, List<String> distractors,
                            String correctAnswer, String justification, String bibliography,
                            String competence, String topic, String subtopic,
                            DifficultyLevel difficultyLevel, String authorLogin) {
        this.type = type;
        this.context = context;
        this.directQuestion = directQuestion;
        this.distractors = distractors;
        this.correctAnswer = correctAnswer;
        this.justification = justification;
        this.bibliography = bibliography;
        this.competence = competence;
        this.topic = topic;
        this.subtopic = subtopic;
        this.difficultyLevel = difficultyLevel;
        this.authorLogin = authorLogin;
    }

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
}
