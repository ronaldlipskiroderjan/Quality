package br.com.grupo5.Quality.dto.request;

import br.com.grupo5.Quality.database.enums.PapelPlano;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record PapeisParticipanteRequestDTO(
        @NotEmpty(message = "Informe ao menos um papel.")
        Set<@NotNull(message = "O papel não pode ser nulo.") PapelPlano> papeis
) {
}
