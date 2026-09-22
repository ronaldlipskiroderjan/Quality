package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CriarNaoConformidadeRequestDTO(
        @NotNull(message = "A resposta da auditoria é obrigatória.")
        UUID respostaId,

        UUID responsavelParticipacaoId,

        @NotNull(message = "A classificação é obrigatória.")
        ClassificacaoNaoConformidade classificacao,

        @Size(max = 3000, message = "A ação corretiva deve possuir no máximo 3000 caracteres.")
        String acaoCorretiva
) {
}
