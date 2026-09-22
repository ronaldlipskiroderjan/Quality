package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AtualizarItemVersaoExecucaoRequestDTO(
        @NotBlank(message = "A pergunta é obrigatória.")
        @Size(max = 1000, message = "A pergunta deve possuir no máximo 1000 caracteres.")
        String descricao,

        ResultadoItem resultado,

        UUID responsavelParticipacaoId,

        @Size(max = 150, message = "O responsável deve possuir no máximo 150 caracteres.")
        String responsavelResolucao,

        ClassificacaoNaoConformidade classificacaoNc,

        @Size(max = 3000, message = "A ação corretiva deve possuir no máximo 3000 caracteres.")
        String acaoCorretiva
) {
}
