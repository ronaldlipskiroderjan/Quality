package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioLoginRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String senha
) {
}
