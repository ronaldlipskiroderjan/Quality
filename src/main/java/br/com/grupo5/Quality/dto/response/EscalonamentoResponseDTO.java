package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.NivelEscalonamento;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EscalonamentoResponseDTO(
        UUID id,
        UUID naoConformidadeId,
        NivelEscalonamento nivel,
        UUID responsavelParticipacaoId,
        String responsavelNome,
        String responsavelEmail,
        UUID auditorParticipacaoId,
        String auditorNome,
        String observacao,
        int prazoHoras,
        OffsetDateTime escalonadoEm,
        OffsetDateTime prazoOriginalEm,
        OffsetDateTime prazoEm,
        OffsetDateTime revisadoEm,
        long versaoRegistro
) {
}
