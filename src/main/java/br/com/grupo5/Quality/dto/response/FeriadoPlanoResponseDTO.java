package br.com.grupo5.Quality.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record FeriadoPlanoResponseDTO(
        UUID id,
        LocalDate data,
        String nome
) {
}
