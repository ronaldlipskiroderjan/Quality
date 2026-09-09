package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequestDTO(
        @NotBlank String senhaAntiga,
        @NotBlank @Size(min = 8, max = 72) String novaSenha,
        @NotBlank String confirmacaoSenha
) {
}
