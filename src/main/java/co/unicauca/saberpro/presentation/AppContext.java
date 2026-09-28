package co.unicauca.saberpro.presentation;

import co.unicauca.saberpro.access.*;
import co.unicauca.saberpro.domain.User;
import co.unicauca.saberpro.microkernel.core.QuestionMicrokernel;
import co.unicauca.saberpro.service.*;

/**
 * Contenedor simple de dependencias para la capa de presentacion (evita un
 * framework de inyeccion externo). Se construye una unica vez al arrancar
 * la aplicacion y se comparte entre todas las ventanas Swing.
 */
public final class AppContext {

    private final UserService userService;
    private final QuestionService questionService;
    private User loggedInUser;

    public AppContext() {
        System.out.println("Base de datos: " + co.unicauca.saberpro.access.AppPaths.getDatabaseUrl());

        IUserRepository userRepository = UserRepositoryFactory.getInstance().getRepository("file");
        DataInitializer.seed(userRepository);

        this.userService = new UserService(userRepository, new DefaultPasswordPolicy(), new PBKDF2PasswordHasher());

        IQuestionRepository questionRepository = QuestionRepositoryFactory.getInstance().getRepository("file");
        QuestionMicrokernel microkernel = new QuestionMicrokernel();
        IEmailService emailService = new SmtpEmailService();

        this.questionService = new QuestionService(questionRepository, microkernel, userService, emailService);

        // Si la conexion/tabla de preguntas no se pudo inicializar (por
        // ejemplo, falta el driver JDBC de SQLite en el classpath), avisar
        // de inmediato en vez de dejar que la app arranque "normal" y que
        // luego "Listar preguntas" se vea vacio sin explicacion.
        String startupError = questionRepository.getLastError();
        if (startupError != null) {
            javax.swing.JOptionPane.showMessageDialog(null,
                    "No fue posible inicializar la base de datos de preguntas.\n"
                            + "La aplicacion continuara, pero es probable que \"Mis preguntas\" no muestre datos.\n\n"
                            + startupError,
                    "Error inicializando la base de datos", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    public UserService getUserService() {
        return userService;
    }

    public QuestionService getQuestionService() {
        return questionService;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User loggedInUser) {
        this.loggedInUser = loggedInUser;
    }
}
