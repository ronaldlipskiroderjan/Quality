package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ItemVersaoExecucaoResponseDTO(
        UUID id,
        int ordem,
        String descricao,
        ResultadoItem resultado,
        String observacao,
        OffsetDateTime ncIdentificadaEm,
        UUID responsavelParticipacaoId,
        String responsavelResolucao,
        ClassificacaoNaoConformidade classificacaoNc,
        String acaoCorretiva,
        OffsetDateTime prazoResolucaoEm,
        OffsetDateTime escalonadoEm,
        OffsetDateTime ncConcluidaEm,
        StatusNaoConformidade statusNc
) {
}
