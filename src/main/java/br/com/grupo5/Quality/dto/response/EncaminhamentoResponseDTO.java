package br.com.grupo5.Quality.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EncaminhamentoResponseDTO(
        UUID id,
        UUID naoConformidadeId,
        int totalDestinatarios,
        OffsetDateTime encaminhadoEm,
        long versaoRegistro
) {
}
