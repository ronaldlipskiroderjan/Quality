package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AutorizarConclusaoAuditoriaRequestDTO(
        @NotBlank(message = "Informe a justificativa da autorização.")
        @Size(max = 2000, message = "A justificativa deve possuir no máximo 2000 caracteres.")
        String justificativa
) {
}
