package co.unicauca.saberpro.question.domain;

import co.unicauca.saberpro.common.contract.DifficultyLevel;

import java.util.List;

/**
 * Datos capturados por el autor (HU01) que llegan desde el API y pasan por la
 * validacion estructural antes de construir una {@link Question}.
 */
public class QuestionRequest {

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
