package co.unicauca.saberpro.user.web;

import co.unicauca.saberpro.common.contract.ApiError;
import co.unicauca.saberpro.user.service.UserService;
import co.unicauca.saberpro.user.web.UserDtos.LoginRequest;
import co.unicauca.saberpro.user.web.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** Autenticacion de usuarios. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesion y devuelve los datos del usuario (sin contrasena)")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userService.login(request.login(), request.password())
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
                        LocalDateTime.now().toString(), 401, "Unauthorized",
                        List.of("Usuario o contrasena incorrectos, o usuario inactivo."), "/api/auth/login")));
    }
}
