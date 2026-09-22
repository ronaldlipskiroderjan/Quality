package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EscalonamentoPainelResponseDTO(
        UUID id,
        UUID naoConformidadeId,
        UUID planoId,
        String planoNome,
        NivelEscalonamento nivel,
        UUID artefatoId,
        String artefatoNome,
        int itemOrdem,
        String pergunta,
        String responsavelResolucaoNome,
        StatusNaoConformidade statusNaoConformidade,
        OffsetDateTime escalonadoEm,
        OffsetDateTime prazoOriginalEm,
        OffsetDateTime prazoEm,
        OffsetDateTime revisadoEm,
        long versaoRegistro
) {
}
