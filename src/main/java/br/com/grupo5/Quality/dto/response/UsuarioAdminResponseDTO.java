package br.com.grupo5.Quality.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioAdminResponseDTO(
        UUID id,
        String nome,
        String email,
        boolean ativo,
        LocalDateTime criadoEm
) {
}
