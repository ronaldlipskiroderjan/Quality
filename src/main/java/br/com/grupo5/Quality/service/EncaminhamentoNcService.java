package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.EncaminhamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EncaminhamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.EncaminhamentoResultado;
import br.com.grupo5.Quality.dto.response.EncaminhamentoResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncaminhamentoNcService {

    private static final int TAMANHO_MAXIMO_CHAVE = 100;

    private final EncaminhamentoNcRepository encaminhamentoRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AcessoPlanoService acessoPlanoService;
    private final CalendarioPrazoService calendarioPrazoService;

    @Transactional
    public EncaminhamentoResultado encaminhar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            String chaveIdempotencia
    ) {
        String chave = normalizarChave(chaveIdempotencia);
        NaoConformidadeEntity naoConformidade =
                naoConformidadeRepository.buscarParaAtualizacao(
                        naoConformidadeId,
                        planoId
                ).orElseThrow(() -> new NotFoundException(
                        "Não conformidade não encontrada."
                ));
        validarAuditor(auth, planoId, naoConformidade);

        Optional<EncaminhamentoNcEntity> existente =
                encaminhamentoRepository
                        .findByNaoConformidadeIdAndChaveIdempotencia(
                                naoConformidadeId,
                                chave
                        );
        if (existente.isPresent()) {
            return new EncaminhamentoResultado(
                    toResponse(existente.get()),
                    false
            );
        }
        if (!naoConformidade.rascunho()) {
            throw new InvalidRequestException(
                    "Somente uma não conformidade em rascunho pode ser encaminhada."
            );
        }
        if (naoConformidade.getAcaoCorretiva() == null
                || naoConformidade.getAcaoCorretiva().isBlank()) {
            throw new InvalidRequestException(
                    "Informe a ação corretiva antes de enviar para resolução."
            );
        }

        List<ParticipacaoPlanoEntity> equipe =
                participacaoRepository.buscarPorPapelNoPlano(
                        planoId,
                        PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
                );
        if (equipe.isEmpty()) {
            throw new InvalidRequestException(
                    "Adicione ao menos um membro à equipe de resolução."
            );
        }
        validarResponsavelNaEquipe(naoConformidade, equipe);

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        EncaminhamentoNcEntity encaminhamento =
                encaminhamentoRepository.saveAndFlush(
                        EncaminhamentoNcEntity.builder()
                                .naoConformidade(naoConformidade)
                                .chaveIdempotencia(chave)
                                .totalDestinatarios(equipe.size())
                                .encaminhadoEm(agora)
                                .build()
                );

        equipe.stream()
                .map(membro -> criarNotificacao(
                        naoConformidade,
                        membro,
                        encaminhamento.getId(),
                        agora
                ))
                .forEach(notificacaoRepository::save);

        naoConformidade.setStatus(StatusNaoConformidade.EM_TRATAMENTO);
        naoConformidade.setEnviadaEm(agora);
        naoConformidade.setPrazoEm(
                calendarioPrazoService.calcularPrazo(
                        planoId,
                        agora,
                        naoConformidade.getPrazoResolucaoHoras()
                )
        );
        naoConformidade.setAtualizadoEm(agora);
        naoConformidadeRepository.save(naoConformidade);

        return new EncaminhamentoResultado(
                toResponse(encaminhamento),
                true
        );
    }

    private void validarAuditor(
            Authentication auth,
            UUID planoId,
            NaoConformidadeEntity naoConformidade
    ) {
        ParticipacaoPlanoEntity auditor = acessoPlanoService
                .buscarParticipacao(auth, planoId, PermissaoPlano.AUDITAR);
        UUID auditorAtribuidoId = naoConformidade.getResposta()
                .getAuditoria()
                .getAuditor()
                .getId();
        if (!auditor.getId().equals(auditorAtribuidoId)) {
            throw new AccessDeniedException(
                    "Somente o auditor da execução pode encaminhar a não conformidade."
            );
        }
    }

    private void validarResponsavelNaEquipe(
            NaoConformidadeEntity naoConformidade,
            List<ParticipacaoPlanoEntity> equipe
    ) {
        ParticipacaoPlanoEntity responsavel =
                naoConformidade.getResponsavel();
        if (responsavel != null
                && equipe.stream().noneMatch(
                        membro -> membro.getId().equals(responsavel.getId())
                )) {
            throw new InvalidRequestException(
                    "O responsável atribuído deve pertencer à equipe de resolução."
            );
        }
    }

    private NotificacaoEntity criarNotificacao(
            NaoConformidadeEntity naoConformidade,
            ParticipacaoPlanoEntity membro,
            UUID encaminhamentoId,
            OffsetDateTime criadaEm
    ) {
        return NotificacaoEntity.builder()
                .destinatario(membro)
                .naoConformidade(naoConformidade)
                .chaveEvento(
                        "ENCAMINHAMENTO:"
                                + encaminhamentoId
                                + ":"
                                + membro.getId()
                )
                .tipo(TipoNotificacao.NAO_CONFORMIDADE_ENCAMINHADA_EQUIPE)
                .titulo("Nova não conformidade recebida")
                .mensagem("Uma nova não conformidade foi enviada para a sua equipe de resolução.")
                .status(StatusNotificacao.NAO_LIDA)
                .criadaEm(criadaEm)
                .build();
    }

    private String normalizarChave(String chave) {
        if (chave == null
                || chave.isBlank()
                || chave.trim().length() > TAMANHO_MAXIMO_CHAVE) {
            throw new InvalidRequestException(
                    "Informe uma Idempotency-Key com até 100 caracteres."
            );
        }
        return chave.trim();
    }

    private EncaminhamentoResponseDTO toResponse(
            EncaminhamentoNcEntity encaminhamento
    ) {
        return new EncaminhamentoResponseDTO(
                encaminhamento.getId(),
                encaminhamento.getNaoConformidade().getId(),
                encaminhamento.getTotalDestinatarios(),
                encaminhamento.getEncaminhadoEm(),
                encaminhamento.getVersaoRegistro()
        );
    }
}
