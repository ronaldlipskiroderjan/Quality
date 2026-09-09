package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlanoRequestDTO(
        @NotBlank @Size(max = 150) String nomeProjeto,
        @NotBlank @Size(max = 30) String versao,
        @NotBlank @Size(max = 2000) String objetivo,
        @NotBlank @Size(max = 5000) String visaoGeral
) {
}
