package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusArtefato;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public record ArtefatoResponseDTO(
        UUID id,
        UUID documentoId,
        String documentoNome,
        String nome,
        String versao,
        LocalDate dataPlanejada,
        StatusArtefato status,
        UUID auditorParticipacaoId,
        UUID auditorUsuarioId,
        String auditorNome,
        String auditorEmail,
        UUID auditoriaId,
        List<DocumentoResponseDTO> documentosReferencia,
        int totalChecklists,
        LocalDateTime criadoEm
) {
}
