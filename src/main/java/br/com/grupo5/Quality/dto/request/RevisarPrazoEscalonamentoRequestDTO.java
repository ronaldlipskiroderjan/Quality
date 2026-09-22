package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record RevisarPrazoEscalonamentoRequestDTO(
        @NotNull OffsetDateTime prazoEm
) {
}
