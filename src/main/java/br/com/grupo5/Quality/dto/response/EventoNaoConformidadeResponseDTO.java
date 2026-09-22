package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.TipoEventoNaoConformidade;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventoNaoConformidadeResponseDTO(
        UUID referenciaId,
        TipoEventoNaoConformidade tipo,
        OffsetDateTime ocorridoEm,
        String titulo,
        String detalhe
) {
}
