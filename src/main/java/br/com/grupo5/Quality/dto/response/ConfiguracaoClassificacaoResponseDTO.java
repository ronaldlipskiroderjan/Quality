package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;

import java.util.UUID;

public record ConfiguracaoClassificacaoResponseDTO(
        UUID id,
        ClassificacaoNaoConformidade classificacao,
        int prazoDias,
        int prazoHoras,
        boolean ativa
) {
}
