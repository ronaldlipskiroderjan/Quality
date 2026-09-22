package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificacaoResponseDTO(
        UUID id,
        UUID planoId,
        UUID naoConformidadeId,
        String planoNome,
        String artefatoNome,
        TipoNotificacao tipo,
        String titulo,
        String mensagem,
        StatusNotificacao status,
        OffsetDateTime criadaEm,
        OffsetDateTime lidaEm,
        long versaoRegistro
) {
}
