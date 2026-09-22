package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.request.CriarEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.request.RevisarPrazoEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoPainelResponseDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
public class EscalonamentoService {

    private static final int TAMANHO_MAXIMO_CHAVE = 100;

    private final EscalonamentoNcRepository escalonamentoRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AcessoPlanoService acessoPlanoService;
    private final CalendarioPrazoService calendarioPrazoService;

    @Transactional
    public CriacaoEscalonamentoResultado criar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            String chaveIdempotencia,
            CriarEscalonamentoRequestDTO dto
    ) {
        String chave = normalizarChave(chaveIdempotencia);
        String observacao = normalizarOpcional(dto.observacao());
        ConfiguracaoEscalonamento configuracao =
                obterConfiguracao(dto.nivel());

        NaoConformidadeEntity naoConformidade = buscarComBloqueio(
                planoId,
                naoConformidadeId
        );
        ParticipacaoPlanoEntity auditor = validarAuditor(
                auth,
                planoId,
                naoConformidade
        );

        Optional<EscalonamentoNcEntity> existente =
                escalonamentoRepository
                        .findByNaoConformidadeIdAndChaveIdempotencia(
                                naoConformidadeId,
                                chave
                        );
        if (existente.isPresent()) {
            validarMesmoConteudo(
                    existente.get(),
                    dto,
                    observacao
            );
            return new CriacaoEscalonamentoResultado(
                    toResponse(existente.get()),
                    false
            );
        }

        validarEstado(naoConformidade, configuracao);
        ParticipacaoPlanoEntity responsavel = buscarResponsavel(
                planoId,
                configuracao
        );
        OffsetDateTime escalonadoEm = agora();
        OffsetDateTime prazoEm = calendarioPrazoService.calcularPrazo(
                planoId,
                escalonadoEm,
                dto.prazoHoras()
        );

        EscalonamentoNcEntity escalonamento =
                EscalonamentoNcEntity.builder()
                        .naoConformidade(naoConformidade)
                        .chaveIdempotencia(chave)
                        .nivel(configuracao.nivel())
                        .responsavel(responsavel)
                        .auditor(auditor)
                        .observacao(observacao)
                        .prazoHoras(dto.prazoHoras())
                        .escalonadoEm(escalonadoEm)
                        .prazoOriginalEm(prazoEm)
                        .prazoEm(prazoEm)
                        .build();
        escalonamento = escalonamentoRepository.saveAndFlush(escalonamento);

        notificacaoRepository.save(criarNotificacao(
                naoConformidade,
                responsavel,
                configuracao,
                escalonadoEm
        ));

        naoConformidade.setStatus(configuracao.statusDestino());
        naoConformidade.setPrazoEm(prazoEm);
        naoConformidade.setAtualizadoEm(escalonadoEm);
        naoConformidadeRepository.save(naoConformidade);

        return new CriacaoEscalonamentoResultado(
                toResponse(escalonamento),
                true
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<EscalonamentoResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        buscarNaoConformidade(planoId, naoConformidadeId);
        return PaginaResponseDTO.de(escalonamentoRepository.listarPorNaoConformidade(
                        planoId,
                        naoConformidadeId,
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public EscalonamentoResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            UUID escalonamentoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return escalonamentoRepository.buscarPorId(
                        planoId,
                        naoConformidadeId,
                        escalonamentoId
                )
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(
                        "Escalonamento não encontrado."
                ));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<EscalonamentoPainelResponseDTO> listarDoSuperior(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        ParticipacaoPlanoEntity superior = validarSuperior(auth, planoId);
        return PaginaResponseDTO.de(escalonamentoRepository.listarDoSuperior(
                planoId,
                superior.getId(),
                pageable
        ).map(this::toPainelResponse));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<EscalonamentoPainelResponseDTO>
            listarAtribuidosAoSuperior(
                    Authentication auth,
                    Pageable pageable
            ) {
        return PaginaResponseDTO.de(escalonamentoRepository
                .listarAtribuidosAoSuperior(
                        auth.getName(),
                        NivelEscalonamento.N1,
                        List.of(
                                StatusNaoConformidade.ESCALONADA_N1,
                                StatusNaoConformidade.VENCIDA_N1
                        ),
                        NivelEscalonamento.N2,
                        List.of(
                                StatusNaoConformidade.ESCALONADA_N2,
                                StatusNaoConformidade.VENCIDA_N2
                        ),
                        pageable
                )
                .map(this::toPainelResponse));
    }

    @Transactional
    public EscalonamentoPainelResponseDTO revisarPrazo(
            Authentication auth,
            UUID planoId,
            UUID escalonamentoId,
            RevisarPrazoEscalonamentoRequestDTO dto
    ) {
        ParticipacaoPlanoEntity superior = validarSuperior(auth, planoId);
        EscalonamentoNcEntity escalonamento = escalonamentoRepository
                .buscarParaRevisao(planoId, superior.getId(), escalonamentoId)
                .orElseThrow(() -> new NotFoundException(
                        "Escalonamento não encontrado."
                ));
        OffsetDateTime novoPrazo = dto.prazoEm();
        OffsetDateTime agora = agora();
        NaoConformidadeEntity naoConformidade =
                escalonamento.getNaoConformidade();
        StatusNaoConformidade statusEsperado =
                escalonamento.getNivel() == NivelEscalonamento.N1
                        ? StatusNaoConformidade.ESCALONADA_N1
                        : StatusNaoConformidade.ESCALONADA_N2;
        if (naoConformidade.getStatus() != statusEsperado) {
            throw new InvalidRequestException(
                    "Este escalonamento não está mais ativo."
            );
        }
        if (!novoPrazo.equals(escalonamento.getPrazoEm())
                && !novoPrazo.isAfter(agora)) {
            throw new InvalidRequestException(
                    "A nova data de resolução deve estar no futuro."
            );
        }

        escalonamento.setPrazoEm(novoPrazo);
        escalonamento.setRevisadoEm(agora);
        naoConformidade.setPrazoEm(novoPrazo);
        naoConformidade.setAtualizadoEm(agora);
        naoConformidadeRepository.save(naoConformidade);
        return toPainelResponse(escalonamentoRepository.save(escalonamento));
    }

    private ParticipacaoPlanoEntity validarSuperior(
            Authentication auth,
            UUID planoId
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(
                        auth,
                        planoId,
                        PermissaoPlano.GERENCIAR_ESCALONAMENTOS
                );
        if (!participacao.possuiPapel(PapelPlano.SUPERIOR_N1)
                && !participacao.possuiPapel(PapelPlano.SUPERIOR_N2)) {
            throw new AccessDeniedException(
                    "Somente superiores podem revisar escalonamentos."
            );
        }
        return participacao;
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

    private ParticipacaoPlanoEntity validarAuditor(
            Authentication auth,
            UUID planoId,
            NaoConformidadeEntity naoConformidade
    ) {
        ParticipacaoPlanoEntity auditor = acessoPlanoService
                .buscarParticipacao(
                        auth,
                        planoId,
                        PermissaoPlano.ESCALONAR_NAO_CONFORMIDADE
                );
        UUID auditorId = naoConformidade.getResposta()
                .getAuditoria()
                .getAuditor()
                .getId();
        if (!auditor.getId().equals(auditorId)) {
            throw new AccessDeniedException(
                    "Somente o auditor da execução pode escalonar a NC."
            );
        }
        return auditor;
    }

    private ParticipacaoPlanoEntity buscarResponsavel(
            UUID planoId,
            ConfiguracaoEscalonamento configuracao
    ) {
        List<ParticipacaoPlanoEntity> responsaveis =
                participacaoRepository.buscarPorPapelNoPlano(
                        planoId,
                        configuracao.papelResponsavel()
                );
        String nivel = configuracao.nivel().name();
        if (responsaveis.isEmpty()) {
            throw new InvalidRequestException(
                    "Defina um responsável " + nivel
                            + " no plano antes de escalonar."
            );
        }
        if (responsaveis.size() > 1) {
            throw new InvalidRequestException(
                    "O plano possui mais de um responsável " + nivel + "."
            );
        }
        return responsaveis.getFirst();
    }

    private ConfiguracaoEscalonamento obterConfiguracao(
            NivelEscalonamento nivel
    ) {
        if (nivel == null) {
            throw new InvalidRequestException(
                    "Informe o nível do escalonamento."
            );
        }
        return switch (nivel) {
            case N1 -> new ConfiguracaoEscalonamento(
                    nivel,
                    StatusNaoConformidade.VENCIDA,
                    StatusNaoConformidade.ESCALONADA_N1,
                    PapelPlano.SUPERIOR_N1,
                    TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N1,
                    "Não conformidade escalonada para N1",
                    "Uma não conformidade vencida foi escalonada ao superior de nível 1."
            );
            case N2 -> new ConfiguracaoEscalonamento(
                    nivel,
                    StatusNaoConformidade.VENCIDA_N1,
                    StatusNaoConformidade.ESCALONADA_N2,
                    PapelPlano.SUPERIOR_N2,
                    TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N2,
                    "Não conformidade escalonada para N2",
                    "Uma não conformidade vencida no N1 foi escalonada ao superior de nível 2."
            );
        };
    }

    private void validarEstado(
            NaoConformidadeEntity naoConformidade,
            ConfiguracaoEscalonamento configuracao
    ) {
        if (naoConformidade.getStatus() != configuracao.statusOrigem()) {
            throw new InvalidRequestException(
                    "O escalonamento " + configuracao.nivel().name()
                            + " exige uma NC no status "
                            + configuracao.statusOrigem().name()
                            + "."
            );
        }
    }

    private void validarMesmoConteudo(
            EscalonamentoNcEntity escalonamento,
            CriarEscalonamentoRequestDTO dto,
            String observacao
    ) {
        if (escalonamento.getNivel() != dto.nivel()
                || escalonamento.getPrazoHoras() != dto.prazoHoras()
                || !java.util.Objects.equals(
                        escalonamento.getObservacao(),
                        observacao
                )) {
            throw new InvalidRequestException(
                    "A Idempotency-Key já foi usada com outro conteúdo."
            );
        }
    }

    private String normalizarChave(String chaveIdempotencia) {
        if (chaveIdempotencia == null
                || chaveIdempotencia.isBlank()
                || chaveIdempotencia.trim().length() > TAMANHO_MAXIMO_CHAVE) {
            throw new InvalidRequestException(
                    "Informe uma Idempotency-Key com até 100 caracteres."
            );
        }
        return chaveIdempotencia.trim();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private NotificacaoEntity criarNotificacao(
            NaoConformidadeEntity naoConformidade,
            ParticipacaoPlanoEntity responsavel,
            ConfiguracaoEscalonamento configuracao,
            OffsetDateTime criadaEm
    ) {
        return NotificacaoEntity.builder()
                .destinatario(responsavel)
                .naoConformidade(naoConformidade)
                .chaveEvento(
                        "ESCALONAMENTO_"
                                + configuracao.nivel().name()
                                + ":"
                                + naoConformidade.getId()
                )
                .tipo(configuracao.tipoNotificacao())
                .titulo(configuracao.titulo())
                .mensagem(configuracao.mensagem())
                .status(StatusNotificacao.NAO_LIDA)
                .criadaEm(criadaEm)
                .build();
    }

    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record ConfiguracaoEscalonamento(
            NivelEscalonamento nivel,
            StatusNaoConformidade statusOrigem,
            StatusNaoConformidade statusDestino,
            PapelPlano papelResponsavel,
            TipoNotificacao tipoNotificacao,
            String titulo,
            String mensagem
    ) {
    }

    private EscalonamentoResponseDTO toResponse(
            EscalonamentoNcEntity escalonamento
    ) {
        ParticipacaoPlanoEntity responsavel = escalonamento.getResponsavel();
        ParticipacaoPlanoEntity auditor = escalonamento.getAuditor();
        return new EscalonamentoResponseDTO(
                escalonamento.getId(),
                escalonamento.getNaoConformidade().getId(),
                escalonamento.getNivel(),
                responsavel.getId(),
                responsavel.getUsuario().getNome(),
                responsavel.getUsuario().getEmail(),
                auditor.getId(),
                auditor.getUsuario().getNome(),
                escalonamento.getObservacao(),
                escalonamento.getPrazoHoras(),
                escalonamento.getEscalonadoEm(),
                escalonamento.getPrazoOriginalEm(),
                escalonamento.getPrazoEm(),
                escalonamento.getRevisadoEm(),
                escalonamento.getVersaoRegistro()
        );
    }

    private EscalonamentoPainelResponseDTO toPainelResponse(
            EscalonamentoNcEntity escalonamento
    ) {
        NaoConformidadeEntity naoConformidade =
                escalonamento.getNaoConformidade();
        var resposta = naoConformidade.getResposta();
        var artefato = resposta.getAuditoria().getArtefato();
        var plano = artefato.getDocumento().getPlano();
        return new EscalonamentoPainelResponseDTO(
                escalonamento.getId(),
                naoConformidade.getId(),
                plano.getId(),
                plano.getNomeProjeto(),
                escalonamento.getNivel(),
                artefato.getId(),
                artefato.getNome(),
                resposta.getItem().getOrdem(),
                resposta.getItem().getPergunta(),
                naoConformidade.getResponsavel() == null
                        ? null
                        : naoConformidade.getResponsavel().getUsuario().getNome(),
                naoConformidade.getStatus(),
                escalonamento.getEscalonadoEm(),
                escalonamento.getPrazoOriginalEm(),
                escalonamento.getPrazoEm(),
                escalonamento.getRevisadoEm(),
                escalonamento.getVersaoRegistro()
        );
    }
}
