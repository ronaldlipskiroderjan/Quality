package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NaoConformidadeResponseDTO(
        UUID id,
        UUID planoId,
        String planoNome,
        UUID respostaId,
        UUID auditoriaId,
        UUID artefatoId,
        String artefatoNome,
        UUID itemId,
        int itemOrdem,
        String pergunta,
        UUID auditorParticipacaoId,
        String auditorNome,
        UUID responsavelParticipacaoId,
        String responsavelNome,
        String responsavelEmail,
        ClassificacaoNaoConformidade classificacao,
        String acaoCorretiva,
        StatusNaoConformidade status,
        OffsetDateTime identificadoEm,
        OffsetDateTime enviadaEm,
        OffsetDateTime prazoEm,
        OffsetDateTime ultimoEscalonamentoEm,
        OffsetDateTime concluidaEm,
        OffsetDateTime atualizadoEm,
        long versaoRegistro
) {
}
