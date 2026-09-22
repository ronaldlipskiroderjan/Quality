package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.dto.response.EscalonamentoResponseDTO;

public record CriacaoEscalonamentoResultado(
        EscalonamentoResponseDTO escalonamento,
        boolean criado
) {
}
