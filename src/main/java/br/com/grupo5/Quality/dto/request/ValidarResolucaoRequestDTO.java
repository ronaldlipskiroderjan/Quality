package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.DecisaoValidacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidarResolucaoRequestDTO(
        @NotNull(message = "A decisão da validação é obrigatória.")
        DecisaoValidacao decisao,

        @Size(
                max = 3000,
                message = "A observação deve ter no máximo 3000 caracteres."
        )
        String observacao
) {
}
