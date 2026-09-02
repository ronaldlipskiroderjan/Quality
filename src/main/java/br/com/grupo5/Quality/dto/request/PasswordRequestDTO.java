package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordRequestDTO(
        @NotBlank String senhaAntiga,
        @NotBlank String novaSenha,
        @NotBlank String confirmacaoSenha
) {
}
