package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InformarResolucaoRequestDTO(
        @NotBlank(message = "A descrição da resolução é obrigatória.")
        @Size(
                max = 5000,
                message = "A descrição deve ter no máximo 5000 caracteres."
        )
        String descricao,

        @Size(
                max = 2000,
                message = "A evidência deve ter no máximo 2000 caracteres."
        )
        String evidencia
) {
}
