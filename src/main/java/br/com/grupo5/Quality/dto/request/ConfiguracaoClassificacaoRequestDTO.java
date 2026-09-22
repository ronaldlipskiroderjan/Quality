package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfiguracaoClassificacaoRequestDTO(
        @NotNull(message = "A classificação é obrigatória.")
        ClassificacaoNaoConformidade classificacao,

        @Min(value = 0, message = "Os dias não podem ser negativos.")
        @Max(value = 365, message = "O prazo deve ser de no máximo 365 dias.")
        int prazoDias,

        @Min(value = 0, message = "As horas não podem ser negativas.")
        @Max(value = 23, message = "Informe entre 0 e 23 horas.")
        int prazoHoras,

        boolean ativa
) {
}
