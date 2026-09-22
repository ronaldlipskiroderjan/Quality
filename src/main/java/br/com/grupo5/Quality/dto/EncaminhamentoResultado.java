package br.com.grupo5.Quality.dto;

import br.com.grupo5.Quality.dto.response.EncaminhamentoResponseDTO;

public record EncaminhamentoResultado(
        EncaminhamentoResponseDTO encaminhamento,
        boolean criado
) {
}
