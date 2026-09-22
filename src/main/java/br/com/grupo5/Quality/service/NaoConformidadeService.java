package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.RespostaAuditoriaRepository;
import br.com.grupo5.Quality.dto.request.AtualizarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.request.CriarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.response.NaoConformidadeResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NaoConformidadeService {

    private final NaoConformidadeRepository naoConformidadeRepository;
    private final RespostaAuditoriaRepository respostaRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final AcessoPlanoService acessoPlanoService;
    private final ConfiguracaoPlanoService configuracaoPlanoService;
    private final EncaminhamentoNcService encaminhamentoNcService;
    private final AcessoNaoConformidadeService acessoNaoConformidadeService;
    private final CalendarioPrazoService calendarioPrazoService;
    @Transactional
    public NaoConformidadeResponseDTO criar(
            Authentication auth,
            UUID planoId,
            CriarNaoConformidadeRequestDTO dto
    ) {
        NaoConformidadeEntity naoConformidade = registrar(
                auth, planoId, dto
        );
        encaminhamentoNcService.encaminhar(
                auth,
                planoId,
                naoConformidade.getId(),
                UUID.randomUUID().toString()
        );
        return toResponse(naoConformidade);
    }

    @Transactional
    public NaoConformidadeResponseDTO criarRascunho(
            Authentication auth,
            UUID planoId,
            CriarNaoConformidadeRequestDTO dto
    ) {
        RespostaAuditoriaEntity resposta = buscarResposta(
                planoId,
                dto.respostaId()
        );
        validarAuditor(auth, planoId, resposta.getAuditoria());
        validarAuditoriaEmAndamento(resposta.getAuditoria());
        validarRespostaNaoConforme(resposta);
        return naoConformidadeRepository.findByRespostaId(resposta.getId())
                .map(this::toResponse)
                .orElseGet(() -> toResponse(registrar(
                        planoId, dto, resposta
                )));
    }

    private NaoConformidadeEntity registrar(
            Authentication auth,
            UUID planoId,
            CriarNaoConformidadeRequestDTO dto
    ) {
        RespostaAuditoriaEntity resposta = buscarResposta(
                planoId,
                dto.respostaId()
        );
        validarAuditor(auth, planoId, resposta.getAuditoria());
        validarAuditoriaEmAndamento(resposta.getAuditoria());
        validarRespostaNaoConforme(resposta);
        return registrar(planoId, dto, resposta);
    }

    private NaoConformidadeEntity registrar(
            UUID planoId,
            CriarNaoConformidadeRequestDTO dto,
            RespostaAuditoriaEntity resposta
    ) {
        AuditoriaEntity auditoria = resposta.getAuditoria();
        auditoria.setConclusaoExcepcionalAutorizadaPor(null);
        auditoria.setConclusaoExcepcionalAutorizadaEm(null);
        auditoria.setJustificativaConclusaoExcepcional(null);

        if (naoConformidadeRepository.existsByRespostaId(resposta.getId())) {
            throw new AlreadyExistsException(
                    "Este item já foi enviado para resolução."
            );
        }

        ParticipacaoPlanoEntity responsavel = buscarResponsavelOpcional(
                planoId, dto.responsavelParticipacaoId());
        int prazoHoras = configuracaoPlanoService
                .buscarAtiva(planoId, dto.classificacao())
                .getPrazoHoras();
        OffsetDateTime agora = agora();

        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .resposta(resposta)
                        .responsavel(responsavel)
                        .classificacao(dto.classificacao())
                        .descricao(resposta.getItem().getPergunta())
                        .acaoCorretiva(normalizar(dto.acaoCorretiva()))
                        .identificadoEm(agora)
                        .prazoResolucaoHoras(prazoHoras)
                        .prazoEm(calendarioPrazoService.calcularPrazo(
                                planoId,
                                agora,
                                prazoHoras
                        ))
                        .atualizadoEm(agora)
                        .status(StatusNaoConformidade.RASCUNHO)
                        .build();

        return naoConformidadeRepository.saveAndFlush(
                naoConformidade
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return PaginaResponseDTO.de(naoConformidadeRepository
                .findAllByRespostaAuditoriaArtefatoDocumentoPlanoId(
                        planoId,
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listarDaEquipe(
            Authentication auth,
            Pageable pageable
    ) {
        return PaginaResponseDTO.de(naoConformidadeRepository
                .listarDaEquipe(
                        auth.getName(),
                        PapelPlano.MEMBRO_EQUIPE_RESOLUCAO,
                        StatusNaoConformidade.RASCUNHO,
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listarAtribuidas(
            Authentication auth,
            Pageable pageable
    ) {
        return PaginaResponseDTO.de(naoConformidadeRepository
                .listarAtribuidasAoResponsavel(
                        auth.getName(),
                        List.of(
                                StatusNaoConformidade.CONCLUIDA,
                                StatusNaoConformidade.CANCELADA
                        ),
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listarDaEquipeNoPlano(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(auth, planoId);
        if (!participacao.possuiPapel(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO)) {
            throw new AccessDeniedException(
                    "Somente membros da equipe de resolução acessam esta caixa."
            );
        }
        return PaginaResponseDTO.de(naoConformidadeRepository
                .listarDaEquipeNoPlano(
                        planoId,
                        auth.getName(),
                        PapelPlano.MEMBRO_EQUIPE_RESOLUCAO,
                        StatusNaoConformidade.RASCUNHO,
                        pageable
                )
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public NaoConformidadeResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId
    ) {
        acessoNaoConformidadeService.validarConsulta(auth, planoId, naoConformidadeId);
        return toResponse(buscarNaoConformidade(
                planoId,
                naoConformidadeId
        ));
    }

    @Transactional
    public NaoConformidadeResponseDTO atualizar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            AtualizarNaoConformidadeRequestDTO dto
    ) {
        NaoConformidadeEntity naoConformidade = buscarNaoConformidade(
                planoId,
                naoConformidadeId
        );
        validarAuditor(
                auth,
                planoId,
                naoConformidade.getResposta().getAuditoria()
        );
        validarRascunho(naoConformidade);

        ParticipacaoPlanoEntity responsavel = buscarResponsavelOpcional(
                planoId, dto.responsavelParticipacaoId());
        int prazoHoras = configuracaoPlanoService
                .buscarAtiva(planoId, dto.classificacao())
                .getPrazoHoras();
        OffsetDateTime agora = agora();

        naoConformidade.setResponsavel(responsavel);
        naoConformidade.setClassificacao(dto.classificacao());
        naoConformidade.setAcaoCorretiva(normalizar(dto.acaoCorretiva()));
        naoConformidade.setPrazoResolucaoHoras(prazoHoras);
        naoConformidade.setPrazoEm(
                calendarioPrazoService.calcularPrazo(
                        planoId,
                        naoConformidade.getIdentificadoEm(),
                        prazoHoras
                )
        );
        naoConformidade.setAtualizadoEm(agora);

        return toResponse(
                naoConformidadeRepository.save(naoConformidade)
        );
    }

    @Transactional
    public void remover(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId
    ) {
        NaoConformidadeEntity naoConformidade = buscarNaoConformidade(
                planoId,
                naoConformidadeId
        );
        validarAuditor(
                auth,
                planoId,
                naoConformidade.getResposta().getAuditoria()
        );
        validarRascunho(naoConformidade);
        validarAuditoriaEmAndamento(
                naoConformidade.getResposta().getAuditoria()
        );
        naoConformidadeRepository.delete(naoConformidade);
    }

    private RespostaAuditoriaEntity buscarResposta(
            UUID planoId,
            UUID respostaId
    ) {
        return respostaRepository
                .findByIdAndAuditoriaArtefatoDocumentoPlanoId(
                        respostaId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Resposta da auditoria não encontrada."
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

    private ParticipacaoPlanoEntity buscarResponsavelOpcional(
            UUID planoId,
            UUID participanteId
    ) {
        if (participanteId == null) {
            return null;
        }
        ParticipacaoPlanoEntity responsavel = participacaoRepository
                .findByIdAndPlanoId(participanteId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Responsável pela resolução não encontrado."
                ));

        if (!responsavel.possuiPapel(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO)) {
            throw new InvalidRequestException(
                    "O responsável atribuído deve pertencer à equipe de resolução."
            );
        }
        return responsavel;
    }

    private void validarAuditor(
            Authentication auth,
            UUID planoId,
            AuditoriaEntity auditoria
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(auth, planoId, PermissaoPlano.AUDITAR);
        if (!participacao.getId().equals(auditoria.getAuditor().getId())) {
            throw new AccessDeniedException(
                    "Somente o auditor da execução pode alterar a não conformidade."
            );
        }
    }

    private void validarAuditoriaEmAndamento(AuditoriaEntity auditoria) {
        if (!auditoria.emAndamento()) {
            throw new InvalidRequestException(
                    "A não conformidade só pode ser alterada durante a auditoria."
            );
        }
    }

    private void validarRespostaNaoConforme(
            RespostaAuditoriaEntity resposta
    ) {
        if (resposta.getResultado() != ResultadoItem.NAO_CONFORME) {
            throw new InvalidRequestException(
                    "Somente respostas não conformes podem originar uma NC."
            );
        }
    }

    private void validarRascunho(
            NaoConformidadeEntity naoConformidade
    ) {
        if (!naoConformidade.rascunho()) {
            throw new InvalidRequestException(
                    "Somente não conformidades em rascunho podem ser alteradas."
            );
        }
    }


    private OffsetDateTime agora() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private NaoConformidadeResponseDTO toResponse(
            NaoConformidadeEntity naoConformidade
    ) {
        RespostaAuditoriaEntity resposta = naoConformidade.getResposta();
        AuditoriaEntity auditoria = resposta.getAuditoria();
        ParticipacaoPlanoEntity auditor = auditoria.getAuditor();
        ParticipacaoPlanoEntity responsavel =
                naoConformidade.getResponsavel();
        OffsetDateTime ultimoEscalonamentoEm = naoConformidade
                .getEscalonamentos()
                .stream()
                .map(escalonamento -> escalonamento.getEscalonadoEm())
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new NaoConformidadeResponseDTO(
                naoConformidade.getId(),
                auditoria.getArtefato().getDocumento().getPlano().getId(),
                auditoria.getArtefato().getDocumento().getPlano().getNomeProjeto(),
                resposta.getId(),
                auditoria.getId(),
                auditoria.getArtefato().getId(),
                auditoria.getArtefato().getNome(),
                resposta.getItem().getId(),
                resposta.getItem().getOrdem(),
                resposta.getItem().getPergunta(),
                auditor.getId(),
                auditor.getUsuario().getNome(),
                responsavel == null ? null : responsavel.getId(),
                responsavel == null ? null : responsavel.getUsuario().getNome(),
                responsavel == null ? null : responsavel.getUsuario().getEmail(),
                naoConformidade.getClassificacao(),
                naoConformidade.getAcaoCorretiva(),
                naoConformidade.getStatus(),
                naoConformidade.getIdentificadoEm(),
                naoConformidade.getEnviadaEm(),
                naoConformidade.getPrazoEm(),
                ultimoEscalonamentoEm,
                naoConformidade.getConcluidaEm(),
                naoConformidade.getAtualizadoEm(),
                naoConformidade.getVersaoRegistro()
        );
    }
}
