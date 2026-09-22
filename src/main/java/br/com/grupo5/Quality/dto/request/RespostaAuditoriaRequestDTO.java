package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.ResultadoItem;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RespostaAuditoriaRequestDTO(
        @NotNull(message = "O resultado é obrigatório.")
        ResultadoItem resultado,

        @Size(max = 3000, message = "A observação deve possuir no máximo 3000 caracteres.")
        String observacao
) {
}
