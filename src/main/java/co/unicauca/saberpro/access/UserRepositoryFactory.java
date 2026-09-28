package co.unicauca.saberpro.access;

/**
 * Fabrica (Singleton) para obtener la implementacion de IUserRepository.
 */
public class UserRepositoryFactory {

    private static UserRepositoryFactory instance;

    private UserRepositoryFactory() {
    }

    public static UserRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new UserRepositoryFactory();
        }
        return instance;
    }

    public IUserRepository getRepository(String type) {
        switch (type) {
            case "memory":
                return new SQLiteUserRepository("jdbc:sqlite::memory:");
            case "file":
            default:
                return new SQLiteUserRepository(AppPaths.getDatabaseUrl());
        }
    }
}