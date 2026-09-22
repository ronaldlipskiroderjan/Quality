package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.ResultadoItem;

import java.time.LocalDateTime;
import java.util.UUID;

public record ItemAuditoriaResponseDTO(
        UUID itemId,
        UUID respostaId,
        int ordem,
        String pergunta,
        ResultadoItem resultado,
        String observacao,
        LocalDateTime respondidoEm,
        LocalDateTime atualizadoEm,
        Long versaoRegistro
) {
}
