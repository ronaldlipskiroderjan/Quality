package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanoDetalhadoResponseDTO(
        UUID id,
        String nomeProjeto,
        String versao,
        String objetivo,
        String visaoGeral,
        Status status,
        LocalDateTime criadoEm
) {
}
