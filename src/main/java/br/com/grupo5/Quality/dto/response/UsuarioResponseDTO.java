package br.com.grupo5.Quality.dto.response;

import java.util.Set;
import java.util.UUID;

public record UsuarioResponseDTO(
        UUID id,
        String nome,
        String email,
        Set<String> roles
) {
}
