package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.Size;

public record SalvarVersaoExecucaoRequestDTO(
        @Size(max = 500, message = "A observação deve possuir no máximo 500 caracteres.")
        String observacao
) {
}
