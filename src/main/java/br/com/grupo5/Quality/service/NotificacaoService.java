package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.NotificacaoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository notificacaoRepository;

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NotificacaoResponseDTO> listar(
            Authentication auth,
            Pageable pageable
    ) {
        return PaginaResponseDTO.de(notificacaoRepository
                .findAllByDestinatarioUsuarioEmailIgnoreCase(
                        auth.getName(),
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(
            Authentication auth,
            UUID notificacaoId
    ) {
        NotificacaoEntity notificacao = notificacaoRepository
                .findByIdAndDestinatarioUsuarioEmailIgnoreCase(
                        notificacaoId,
                        auth.getName()
                )
                .orElseThrow(() -> new NotFoundException(
                        "Notificação não encontrada."
                ));

        if (!notificacao.lida()) {
            notificacao.setStatus(StatusNotificacao.LIDA);
            notificacao.setLidaEm(OffsetDateTime.now(ZoneOffset.UTC));
            notificacao = notificacaoRepository.save(notificacao);
        }

        return toResponse(notificacao);
    }

    private NotificacaoResponseDTO toResponse(
            NotificacaoEntity notificacao
    ) {
        var plano = notificacao.getDestinatario().getPlano();
        String artefatoNome = "Auditoria";
        var resposta = notificacao.getNaoConformidade().getResposta();
        if (resposta != null
                && resposta.getAuditoria() != null
                && resposta.getAuditoria().getArtefato() != null) {
            artefatoNome = resposta.getAuditoria().getArtefato().getNome();
        }
        return new NotificacaoResponseDTO(
                notificacao.getId(),
                plano.getId(),
                notificacao.getNaoConformidade().getId(),
                plano.getNomeProjeto() == null
                        ? "Plano de qualidade"
                        : plano.getNomeProjeto(),
                artefatoNome,
                notificacao.getTipo(),
                notificacao.getTitulo(),
                notificacao.getMensagem(),
                notificacao.getStatus(),
                notificacao.getCriadaEm(),
                notificacao.getLidaEm(),
                notificacao.getVersaoRegistro()
        );
    }
}
