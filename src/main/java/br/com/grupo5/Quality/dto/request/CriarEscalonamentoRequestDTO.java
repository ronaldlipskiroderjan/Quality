package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEscalonamentoRequestDTO(
        @NotNull(message = "O nível do escalonamento é obrigatório.")
        NivelEscalonamento nivel,

        @Min(value = 1, message = "O prazo deve possuir ao menos uma hora.")
        @Max(value = 8760, message = "O prazo deve possuir no máximo 8760 horas.")
        int prazoHoras,

        @Size(
                max = 3000,
                message = "A observação deve possuir no máximo 3000 caracteres."
        )
        String observacao
) {
}
