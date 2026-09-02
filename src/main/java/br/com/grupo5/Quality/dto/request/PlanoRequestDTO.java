package br.com.grupo5.Quality.dto.request;

public record PlanoRequestDTO(
        String criador,
        String nomeProjeto,
        String versao,
        String objetivo,
        String visaoGeral,
        String status
) {
}
