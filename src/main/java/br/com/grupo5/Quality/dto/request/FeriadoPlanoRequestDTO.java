package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FeriadoPlanoRequestDTO(
        @NotNull(message = "A data do feriado é obrigatória.")
        LocalDate data,

        @NotBlank(message = "O nome do feriado é obrigatório.")
        @Size(max = 120, message = "O nome do feriado deve possuir no máximo 120 caracteres.")
        String nome
) {
}
