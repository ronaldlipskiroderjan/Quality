package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusArtefato;

import java.time.LocalDate;
import java.util.UUID;

public record AuditoriaAgendaResponseDTO(
        UUID artefatoId,
        UUID auditoriaId,
        UUID planoId,
        String planoNome,
        String artefatoNome,
        String versao,
        LocalDate dataPlanejada,
        StatusArtefato status,
        String auditorNome
) {
}
