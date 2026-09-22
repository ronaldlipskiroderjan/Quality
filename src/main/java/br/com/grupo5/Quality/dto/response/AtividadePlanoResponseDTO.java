package br.com.grupo5.Quality.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AtividadePlanoResponseDTO(
        UUID id,
        UUID autorParticipacaoId,
        String autorNome,
        String acao,
        String descricao,
        OffsetDateTime criadoEm
) {
}
