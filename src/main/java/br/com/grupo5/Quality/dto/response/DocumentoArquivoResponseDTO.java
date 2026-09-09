package br.com.grupo5.Quality.dto.response;

public record DocumentoArquivoResponseDTO(
        String nome,
        String tipo,
        byte[] conteudo
) {
}
