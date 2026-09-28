package co.unicauca.saberpro.access;

/**
 * Fabrica (Singleton) que instancia SQLiteUserRepository o cualquier otra
 * implementacion de IUserRepository que se cree en el futuro. El cliente le
 * pide a la fabrica una abstraccion (IUserRepository) y la fabrica decide
 * que implementacion concreta entregar (Inversion de Dependencias).
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

    /**
     * @param type "file"   -> SQLite en un archivo fisico fijo dentro de
     *                        ~/BancoPreguntasSaberPro/bancopreguntas.db,
     *                        sin importar desde que carpeta se ejecute la app.
     *             "memory" -> SQLite en memoria (util para pruebas)
     */
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
