package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ParticipanteResponseDTO(
        UUID id,
        UUID usuarioId,
        String nome,
        String email,
        Set<PapelPlano> papeis,
        Set<PermissaoPlano> permissoes,
        LocalDateTime criadoEm
) {
}
