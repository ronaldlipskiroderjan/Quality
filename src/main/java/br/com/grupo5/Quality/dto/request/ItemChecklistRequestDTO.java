package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ItemChecklistRequestDTO(
        @Min(value = 1, message = "A ordem deve ser maior que zero.")
        int ordem,

        @Size(max = 1000, message = "A pergunta deve possuir no máximo 1000 caracteres.")
        String pergunta
) {
}
