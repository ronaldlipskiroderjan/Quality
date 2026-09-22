package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.PapelPlano;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PapeisParticipanteRequestDTO(
        @NotNull(message = "O papel é obrigatório.")
        PapelPlano papel
) {
    public PapeisParticipanteRequestDTO(Set<PapelPlano> papeis) {
        this(papeis != null && papeis.size() == 1
                ? papeis.iterator().next()
                : null);
    }
}
