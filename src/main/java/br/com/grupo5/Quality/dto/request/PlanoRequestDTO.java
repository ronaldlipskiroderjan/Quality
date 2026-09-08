package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlanoRequestDTO(
        @NotBlank String criador,
        @NotBlank String nomeProjeto,
        @NotBlank String versao,
        @NotBlank String objetivo,
        @NotBlank String visaoGeral
) {
}
