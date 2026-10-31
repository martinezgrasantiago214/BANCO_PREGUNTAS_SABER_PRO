package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.web.BusinessException;
import co.unicauca.saberpro.common.web.ResourceNotFoundException;
import co.unicauca.saberpro.user.domain.UserStatus;
import co.unicauca.saberpro.user.service.OperationResult;
import co.unicauca.saberpro.user.service.UserService;
import co.unicauca.saberpro.user.web.UserDtos.RegisterUserRequest;
import co.unicauca.saberpro.user.web.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta y registro de usuarios. Otros servicios (p. ej. el
 * notification-service) usan GET /api/users/{login} para obtener el correo
 * de un usuario en lugar de leer esta base de datos.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuarios")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un usuario (queda activo)")
    public UserResponse register(@RequestBody RegisterUserRequest request) {
        OperationResult result = userService.register(request.login(), request.fullName(), request.email(),
                request.role(), UserStatus.ACTIVO, request.password());
        if (!result.isSuccess()) {
            throw new BusinessException(result.getErrors());
        }
        return find(request.login());
    }

    @GetMapping
    @Operation(summary = "Lista todos los usuarios")
    public List<UserResponse> list() {
        return userService.listUsers().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/reviewers")
    @Operation(summary = "Lista los revisores activos (HU04)")
    public List<UserResponse> reviewers() {
        return userService.listReviewers().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{login}")
    @Operation(summary = "Consulta un usuario por su login")
    public UserResponse find(@PathVariable String login) {
        return userService.findByLogin(login).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario " + login));
    }
}
