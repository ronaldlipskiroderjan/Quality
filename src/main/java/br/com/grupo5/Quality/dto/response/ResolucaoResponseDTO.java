package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusResolucao;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResolucaoResponseDTO(
        UUID id,
        UUID naoConformidadeId,
        UUID responsavelParticipacaoId,
        String responsavelNome,
        String descricao,
        String evidencia,
        StatusResolucao status,
        OffsetDateTime informadaEm,
        UUID auditorParticipacaoId,
        String auditorNome,
        String observacaoAuditor,
        OffsetDateTime validadaEm,
        long versaoRegistro
) {
}
