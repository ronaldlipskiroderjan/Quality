package br.com.grupo5.Quality.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record PlanoResponseDTO(
        UUID id,
        String criador,
        String nomeProjeto,
        String versao,
        String objetivo,
        String visaoGeral,
        String status,
        LocalDateTime criadoEm
) {
}
