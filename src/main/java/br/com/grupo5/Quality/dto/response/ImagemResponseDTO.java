package br.com.grupo5.Quality.dto.response;

public record ImagemResponseDTO(
        String contentType,
        byte[] imagem
) {
}
