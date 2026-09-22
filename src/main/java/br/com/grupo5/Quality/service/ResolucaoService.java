package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import br.com.grupo5.Quality.database.enums.DecisaoValidacao;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusResolucao;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ResolucaoNcRepository;
import br.com.grupo5.Quality.dto.request.InformarResolucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.ValidarResolucaoRequestDTO;
import br.com.grupo5.Quality.dto.response.ResolucaoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResolucaoService {

    private static final Set<StatusNaoConformidade> STATUS_TRATAVEIS =
            EnumSet.of(
                    StatusNaoConformidade.ENVIADA,
                    StatusNaoConformidade.EM_TRATAMENTO,
                    StatusNaoConformidade.VENCIDA,
                    StatusNaoConformidade.ESCALONADA_N1,
                    StatusNaoConformidade.VENCIDA_N1,
                    StatusNaoConformidade.ESCALONADA_N2,
                    StatusNaoConformidade.VENCIDA_N2
            );

    private final ResolucaoNcRepository resolucaoRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final AcessoPlanoService acessoPlanoService;

    private final AcessoNaoConformidadeService acessoNaoConformidadeService;
    private final AuditoriaService auditoriaService;
    private final NotificacaoResolucaoService notificacaoResolucaoService;

    @Transactional
    public ResolucaoResponseDTO informar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            InformarResolucaoRequestDTO dto
    ) {
        NaoConformidadeEntity naoConformidade = buscarComBloqueio(
                planoId,
                naoConformidadeId
        );
        ParticipacaoPlanoEntity responsavel = acessoPlanoService
                .buscarParticipacao(
                        auth,
                        planoId,
                        PermissaoPlano.TRATAR_NAO_CONFORMIDADE
                );
        validarResponsavel(responsavel, naoConformidade);
        validarStatusParaInformar(naoConformidade);
        if (naoConformidade.getResponsavel() == null) {
            naoConformidade.setResponsavel(responsavel);
        }

        OffsetDateTime informadaEm = agora();
        ResolucaoNcEntity resolucao = ResolucaoNcEntity.builder()
                .naoConformidade(naoConformidade)
                .responsavel(responsavel)
                .descricao(dto.descricao().trim())
                .evidencia(normalizarOpcional(dto.evidencia()))
                .status(StatusResolucao.INFORMADA)
                .informadaEm(informadaEm)
                .build();

        resolucao = resolucaoRepository.save(resolucao);
        naoConformidade.setStatus(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        naoConformidade.setAtualizadoEm(informadaEm);
        naoConformidadeRepository.save(naoConformidade);
        notificacaoResolucaoService.notificarResolucaoInformada(
                planoId,
                naoConformidade,
                resolucao,
                informadaEm
        );

        return toResponse(resolucao);
    }

    @Transactional
    public ResolucaoResponseDTO validar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            UUID resolucaoId,
            ValidarResolucaoRequestDTO dto
    ) {
        NaoConformidadeEntity naoConformidade = buscarComBloqueio(
                planoId,
                naoConformidadeId
        );
        ParticipacaoPlanoEntity auditor = validarAuditor(
                auth,
                planoId,
                naoConformidade
        );
        validarAguardandoResolucao(naoConformidade);

        ResolucaoNcEntity resolucao = buscarResolucao(
                naoConformidadeId,
                resolucaoId
        );
        if (!resolucao.aguardaValidacao()) {
            throw new InvalidRequestException(
                    "A resolução já foi validada."
            );
        }

        String observacao = normalizarOpcional(dto.observacao());
        validarObservacao(dto.decisao(), observacao);
        OffsetDateTime validadaEm = agora();

        resolucao.setAuditor(auditor);
        resolucao.setObservacaoAuditor(observacao);
        resolucao.setValidadaEm(validadaEm);

        if (dto.decisao() == DecisaoValidacao.APROVAR) {
            resolucao.setStatus(StatusResolucao.APROVADA);
            naoConformidade.setStatus(StatusNaoConformidade.CONCLUIDA);
            naoConformidade.setConcluidaEm(validadaEm);
            auditoriaService.marcarConformeAposResolucao(naoConformidade);
        } else {
            resolucao.setStatus(StatusResolucao.AJUSTES_SOLICITADOS);
            naoConformidade.setStatus(
                    StatusNaoConformidade.EM_TRATAMENTO
            );
            naoConformidade.setConcluidaEm(null);
        }

        naoConformidade.setAtualizadoEm(validadaEm);
        resolucaoRepository.save(resolucao);
        naoConformidadeRepository.save(naoConformidade);
        if (dto.decisao() == DecisaoValidacao.SOLICITAR_AJUSTES) {
            notificacaoResolucaoService.notificarAjustesSolicitados(
                    planoId,
                    naoConformidade,
                    resolucao,
                    validadaEm
            );
        }

        return toResponse(resolucao);
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<ResolucaoResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            Pageable pageable
    ) {
        acessoNaoConformidadeService.validarConsulta(auth, planoId, naoConformidadeId);
        buscarNaoConformidade(planoId, naoConformidadeId);

        return PaginaResponseDTO.de(resolucaoRepository
                .findAllByNaoConformidadeId(
                        naoConformidadeId,
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ResolucaoResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            UUID resolucaoId
    ) {
        acessoNaoConformidadeService.validarConsulta(auth, planoId, naoConformidadeId);
        buscarNaoConformidade(planoId, naoConformidadeId);
        return toResponse(buscarResolucao(
                naoConformidadeId,
                resolucaoId
        ));
    }

    private NaoConformidadeEntity buscarComBloqueio(
            UUID planoId,
            UUID naoConformidadeId
    ) {
        return naoConformidadeRepository.buscarParaAtualizacao(
                        naoConformidadeId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Não conformidade não encontrada."
                ));
    }

    private NaoConformidadeEntity buscarNaoConformidade(
            UUID planoId,
            UUID naoConformidadeId
    ) {
        return naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        naoConformidadeId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Não conformidade não encontrada."
                ));
    }

    private ResolucaoNcEntity buscarResolucao(
            UUID naoConformidadeId,
            UUID resolucaoId
    ) {
        return resolucaoRepository
                .findByIdAndNaoConformidadeId(
                        resolucaoId,
                        naoConformidadeId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Resolução não encontrada."
                ));
    }

    private void validarResponsavel(
            ParticipacaoPlanoEntity participacao,
            NaoConformidadeEntity naoConformidade
    ) {
        ParticipacaoPlanoEntity atribuido = naoConformidade.getResponsavel();
        if (atribuido != null
                && !participacao.getId().equals(atribuido.getId())) {
            throw new AccessDeniedException(
                    "Somente o responsável atribuído pode informar a resolução."
            );
        }
    }

    private ParticipacaoPlanoEntity validarAuditor(
            Authentication auth,
            UUID planoId,
            NaoConformidadeEntity naoConformidade
    ) {
        ParticipacaoPlanoEntity auditor = acessoPlanoService
                .buscarParticipacao(
                        auth,
                        planoId,
                        PermissaoPlano.AUDITAR
                );
        UUID auditorId = naoConformidade.getResposta()
                .getAuditoria()
                .getAuditor()
                .getId();
        if (!auditor.getId().equals(auditorId)) {
            throw new AccessDeniedException(
                    "Somente o auditor da execução pode validar a resolução."
            );
        }
        return auditor;
    }

    private void validarStatusParaInformar(
            NaoConformidadeEntity naoConformidade
    ) {
        if (!STATUS_TRATAVEIS.contains(naoConformidade.getStatus())) {
            throw new InvalidRequestException(
                    "O estado atual da NC não permite informar uma resolução."
            );
        }
    }

    private void validarAguardandoResolucao(
            NaoConformidadeEntity naoConformidade
    ) {
        if (naoConformidade.getStatus()
                != StatusNaoConformidade.RESOLUCAO_INFORMADA) {
            throw new InvalidRequestException(
                    "A NC não possui resolução aguardando validação."
            );
        }
    }

    private void validarObservacao(
            DecisaoValidacao decisao,
            String observacao
    ) {
        if (decisao == DecisaoValidacao.SOLICITAR_AJUSTES
                && observacao == null) {
            throw new InvalidRequestException(
                    "Informe a observação para solicitar ajustes."
            );
        }
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.trim();
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private ResolucaoResponseDTO toResponse(
            ResolucaoNcEntity resolucao
    ) {
        ParticipacaoPlanoEntity auditor = resolucao.getAuditor();

        return new ResolucaoResponseDTO(
                resolucao.getId(),
                resolucao.getNaoConformidade().getId(),
                resolucao.getResponsavel().getId(),
                resolucao.getResponsavel().getUsuario().getNome(),
                resolucao.getDescricao(),
                resolucao.getEvidencia(),
                resolucao.getStatus(),
                resolucao.getInformadaEm(),
                auditor == null ? null : auditor.getId(),
                auditor == null ? null : auditor.getUsuario().getNome(),
                resolucao.getObservacaoAuditor(),
                resolucao.getValidadaEm(),
                resolucao.getVersaoRegistro()
        );
    }
}
