package co.unicauca.saberpro.frontend.client;

import co.unicauca.saberpro.common.contract.Role;
import co.unicauca.saberpro.frontend.model.Models.UserView;

import java.util.List;

/** Operaciones del user-service (via gateway). */
public interface UserApi {

    UserView login(String login, String password);

    UserView register(String login, String fullName, String email, Role role, String password);

    List<UserView> listReviewers();
}
