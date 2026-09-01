package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.config.TokenProvider;
import br.com.grupo5.Quality.dto.request.UsuarioLoginRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioRequestDTO;
import br.com.grupo5.Quality.dto.response.TokenResponseDTO;
import br.com.grupo5.Quality.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@RequestBody UsuarioRequestDTO dto) throws Exception {
        authService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TokenResponseDTO login(@RequestBody UsuarioLoginRequestDTO dto) throws Exception {
        return authService.login(dto);
    }

    @PatchMapping("/refresh")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public TokenResponseDTO refreshToken(Authentication authentication) {
        return authService.refreshToken(authentication);
    }
}
