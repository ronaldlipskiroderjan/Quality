package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemChecklistResponseDTO(
        UUID id,
        int ordem,
        String pergunta,
        OrigemItemChecklist origem,
        LocalDateTime criadoEm
) {
}
