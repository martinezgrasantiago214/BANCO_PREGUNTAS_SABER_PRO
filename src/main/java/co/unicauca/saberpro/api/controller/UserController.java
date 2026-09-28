package co.unicauca.saberpro.api.controller;

import co.unicauca.saberpro.api.dto.UserResponseDTO;
import co.unicauca.saberpro.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta de usuarios (solo lectura). Util para saber que revisores asignar (HU04). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public synchronized List<UserResponseDTO> listUsers() {
        return userService.listUsers().stream().map(UserResponseDTO::from).toList();
    }

    @GetMapping("/reviewers")
    public synchronized List<UserResponseDTO> listReviewers() {
        return userService.listReviewers().stream().map(UserResponseDTO::from).toList();
    }
}
