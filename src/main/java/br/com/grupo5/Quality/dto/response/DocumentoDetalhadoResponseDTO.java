package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.Classificacao;

import java.util.UUID;

public record DocumentoDetalhadoResponseDTO(
        UUID id,
        UUID planoId,
        String nome,
        String nomeArquivo,
        String versao,
        String tipoArquivo,
        long tamanho,
        Classificacao classificacao
) {
}
