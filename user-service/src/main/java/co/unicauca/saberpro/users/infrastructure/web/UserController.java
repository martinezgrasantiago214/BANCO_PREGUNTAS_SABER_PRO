package co.unicauca.saberpro.users.infrastructure.web;

import co.unicauca.saberpro.users.application.UserService;
import co.unicauca.saberpro.users.domain.model.Role;
import co.unicauca.saberpro.users.infrastructure.web.dto.RegisterUserRequest;
import co.unicauca.saberpro.users.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @Operation(summary = "Registra un usuario (administrador, autor o revisor)")
    public UserResponse register(@RequestBody RegisterUserRequest r) {
        return UserResponse.from(userService.register(r.login(), r.fullName(), r.email(), r.role(), r.password()));
    }

    @GetMapping
    @Operation(summary = "Lista usuarios, opcionalmente filtrados por rol (ej. role=REVISOR)")
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.list(role).stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{login}")
    @Operation(summary = "Consulta un usuario por login (usado por otros microservicios)")
    public UserResponse get(@PathVariable String login) {
        return UserResponse.from(userService.findByLogin(login));
    }
}
