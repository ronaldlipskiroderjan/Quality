package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanoResponseDTO(
        UUID id,
        String nomeProjeto,
        String versao,
        Status status,
        LocalDateTime criadoEm
) {
}
