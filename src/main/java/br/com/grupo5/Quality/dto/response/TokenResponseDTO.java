package br.com.grupo5.Quality.dto.response;

public record TokenResponseDTO(
        String token,
        long expiration
) {
}
