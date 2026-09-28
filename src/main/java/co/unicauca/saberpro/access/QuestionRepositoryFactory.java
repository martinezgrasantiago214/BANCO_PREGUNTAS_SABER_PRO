package co.unicauca.saberpro.access;

/** Fabrica (Singleton) para obtener la implementacion de IQuestionRepository. */
public class QuestionRepositoryFactory {

    private static QuestionRepositoryFactory instance;

    private QuestionRepositoryFactory() {
    }

    public static QuestionRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new QuestionRepositoryFactory();
        }
        return instance;
    }

    public IQuestionRepository getRepository(String type) {
        switch (type) {
            case "memory":
                return new SQLiteQuestionRepository("jdbc:sqlite::memory:");
            case "file":
            default:
                return new SQLiteQuestionRepository(AppPaths.getDatabaseUrl());
        }
    }
}
