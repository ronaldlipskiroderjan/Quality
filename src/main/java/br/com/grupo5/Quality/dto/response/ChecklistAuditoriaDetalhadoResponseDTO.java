package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusChecklist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ChecklistAuditoriaDetalhadoResponseDTO(
        UUID id,
        UUID auditoriaId,
        UUID artefatoId,
        String versao,
        StatusChecklist status,
        int totalItens,
        List<ItemAuditoriaResponseDTO> itens,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        long versaoRegistro
) {
}
