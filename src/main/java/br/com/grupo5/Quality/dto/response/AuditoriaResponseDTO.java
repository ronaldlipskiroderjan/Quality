package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusAuditoria;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record AuditoriaResponseDTO(
        UUID id,
        UUID artefatoId,
        UUID auditorParticipacaoId,
        String auditorNome,
        StatusAuditoria status,
        int totalChecklists,
        int totalItens,
        int totalRespondidos,
        int conformes,
        int naoConformes,
        int naoAplicaveis,
        BigDecimal aderenciaPercentual,
        List<DocumentoResponseDTO> documentosReferencia,
        List<ChecklistResponseDTO> checklists,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        long versaoRegistro
) {
}
