package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.Email;

public record SuperioresPlanoRequestDTO(
        @Email(message = "Informe um e-mail válido para o superior N1.")
        String superiorN1Email,

        @Email(message = "Informe um e-mail válido para o superior N2.")
        String superiorN2Email
) {
}
