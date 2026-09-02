package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioUpdateRequestDTO(
        @NotBlank String nome,
        @NotBlank @Email String email
) {
}
