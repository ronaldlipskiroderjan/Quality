package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.UsuarioLoginRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioRequestDTO;
import br.com.grupo5.Quality.dto.response.TokenResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import br.com.grupo5.Quality.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    @SecurityRequirements

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO registrar(@Valid @RequestBody UsuarioRequestDTO dto) {
        return authService.registrar(dto);
    }
    @SecurityRequirements

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDTO autenticar(@Valid @RequestBody UsuarioLoginRequestDTO dto) {
        return authService.autenticar(dto);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponseDTO renovar(Authentication auth) {
        return authService.renovar(auth);
    }
}
