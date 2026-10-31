package co.unicauca.saberpro.users.infrastructure.web;

import co.unicauca.saberpro.users.application.UserService;
import co.unicauca.saberpro.users.infrastructure.web.dto.LoginRequest;
import co.unicauca.saberpro.users.infrastructure.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve los datos y el rol del usuario")
    public UserResponse login(@Valid @RequestBody LoginRequest request) {
        return UserResponse.from(userService.login(request.login(), request.password()));
    }
}
