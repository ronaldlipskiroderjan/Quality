package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.ItemVersaoExecucaoEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.VersaoExecucaoChecklistEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusChecklist;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.TipoVersaoExecucaoChecklist;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.ChecklistRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.VersaoExecucaoChecklistRepository;
import br.com.grupo5.Quality.dto.request.AtualizarItemVersaoExecucaoRequestDTO;
import br.com.grupo5.Quality.dto.response.ItemVersaoExecucaoResponseDTO;
import br.com.grupo5.Quality.dto.response.VersaoExecucaoChecklistResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecucaoChecklistService {

    private static final Set<StatusNaoConformidade> STATUS_FINAIS = Set.of(
            StatusNaoConformidade.CONCLUIDA,
            StatusNaoConformidade.CANCELADA
    );

    private final AuditoriaRepository auditoriaRepository;
    private final ChecklistRepository checklistRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final VersaoExecucaoChecklistRepository versaoRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final AcessoPlanoService acessoPlanoService;
    private final ConfiguracaoPlanoService configuracaoPlanoService;
    private final CalendarioPrazoService calendarioPrazoService;

    @Transactional
    public VersaoExecucaoChecklistResponseDTO salvarVersao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            String observacao
    ) {
        ContextoExecucao contexto = buscarParaAtualizacao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        validarEmExecucao(contexto);
        return salvarSnapshot(
                contexto.checklist(),
                contexto.participacao(),
                TipoVersaoExecucaoChecklist.MANUAL,
                normalizar(observacao)
        );
    }

    @Transactional
    public VersaoExecucaoChecklistResponseDTO encerrar(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId
    ) {
        ContextoExecucao contexto = buscarParaAtualizacao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        validarEmExecucao(contexto);
        validarEncerramento(contexto.auditoria(), contexto.checklist());
        contexto.checklist().setStatus(StatusChecklist.CONCLUIDO);
        contexto.checklist().setAtualizadoEm(LocalDateTime.now());
        checklistRepository.save(contexto.checklist());
        return salvarSnapshot(
                contexto.checklist(),
                contexto.participacao(),
                TipoVersaoExecucaoChecklist.ENCERRAMENTO,
                "Versão final do checklist."
        );
    }

    @Transactional
    public void autorizarConclusaoExcepcional(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            String justificativa
    ) {
        ParticipacaoPlanoEntity superior = acessoPlanoService
                .buscarParticipacao(auth, planoId);
        if (!superior.possuiPapel(PapelPlano.SUPERIOR_N1)
                && !superior.possuiPapel(PapelPlano.SUPERIOR_N2)) {
            throw new AccessDeniedException(
                    "Somente um superior N1 ou N2 pode autorizar a conclusão excepcional."
            );
        }
        AuditoriaEntity auditoria = buscarAuditoria(
                planoId, artefatoId, auditoriaId
        );
        if (auditoria.getStatus() != StatusAuditoria.EM_ANDAMENTO) {
            throw new InvalidRequestException(
                    "Somente auditorias em andamento podem receber esta autorização."
            );
        }
        List<NaoConformidadeEntity> pendentes = naoConformidadeRepository
                .findAllByRespostaAuditoriaId(auditoriaId)
                .stream()
                .filter(this::naoResolvida)
                .toList();
        if (pendentes.isEmpty()) {
            throw new InvalidRequestException(
                    "Não existem não conformidades pendentes para autorizar."
            );
        }
        if (pendentes.stream().anyMatch(nc -> nc.getEscalonamentos().isEmpty())) {
            throw new InvalidRequestException(
                    "Toda não conformidade pendente deve possuir um escalonamento."
            );
        }

        auditoria.setConclusaoExcepcionalAutorizadaPor(superior);
        auditoria.setConclusaoExcepcionalAutorizadaEm(LocalDateTime.now());
        auditoria.setJustificativaConclusaoExcepcional(justificativa.trim());
        auditoriaRepository.save(auditoria);
    }

    @Transactional(readOnly = true)
    public List<VersaoExecucaoChecklistResponseDTO> listarVersoes(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        buscarChecklist(planoId, artefatoId, auditoriaId, checklistId);
        return versaoRepository
                .findAllByChecklistIdOrderByNumeroDesc(checklistId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ItemVersaoExecucaoResponseDTO atualizarItemVersao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            UUID versaoId,
            UUID itemId,
            AtualizarItemVersaoExecucaoRequestDTO dto
    ) {
        ContextoExecucao contexto = buscarParaAtualizacao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        validarEmExecucao(contexto);
        VersaoExecucaoChecklistEntity versao = versaoRepository
                .findByIdAndChecklistIdAndChecklistAuditoriaIdAndChecklistAuditoriaArtefatoIdAndChecklistAuditoriaArtefatoDocumentoPlanoId(
                        versaoId,
                        checklistId,
                        auditoriaId,
                        artefatoId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Versão do checklist não encontrada."
                ));
        ItemVersaoExecucaoEntity item = versao.getItens().stream()
                .filter(registro -> registro.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Item da versão não encontrado."
                ));

        item.setDescricao(dto.descricao().trim());
        item.setResultado(dto.resultado());
        if (dto.resultado() != ResultadoItem.NAO_CONFORME) {
            limparDadosNaoConformidade(item);
        } else {
            atualizarDadosNaoConformidade(item, planoId, dto);
        }
        versaoRepository.save(versao);
        return toItemResponse(item);
    }

    @Transactional
    public VersaoExecucaoChecklistResponseDTO removerItemVersao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            UUID versaoId,
            UUID itemId
    ) {
        ContextoExecucao contexto = buscarParaAtualizacao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        validarEmExecucao(contexto);
        VersaoExecucaoChecklistEntity versao = versaoRepository
                .findByIdAndChecklistIdAndChecklistAuditoriaIdAndChecklistAuditoriaArtefatoIdAndChecklistAuditoriaArtefatoDocumentoPlanoId(
                        versaoId,
                        checklistId,
                        auditoriaId,
                        artefatoId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Versão do checklist não encontrada."
                ));
        boolean removido = versao.getItens().removeIf(
                item -> item.getId().equals(itemId)
        );
        if (!removido) {
            throw new NotFoundException("Item da versão não encontrado.");
        }
        versao.getItens().sort(Comparator.comparingInt(
                ItemVersaoExecucaoEntity::getOrdem
        ));
        for (int indice = 0; indice < versao.getItens().size(); indice++) {
            versao.getItens().get(indice).setOrdem(indice + 1);
        }
        return toResponse(versaoRepository.save(versao));
    }

    private void validarEmExecucao(ContextoExecucao contexto) {
        if (contexto.auditoria().getStatus() != StatusAuditoria.EM_ANDAMENTO
                || contexto.checklist().getStatus() != StatusChecklist.PUBLICADO) {
            throw new InvalidRequestException(
                    "O checklist precisa estar aberto para execução."
            );
        }
    }

    private void validarEncerramento(
            AuditoriaEntity auditoria,
            ChecklistEntity checklist
    ) {
        Map<UUID, RespostaAuditoriaEntity> respostas = respostas(checklist);
        if (respostas.size() != checklist.getItens().size()) {
            throw new InvalidRequestException(
                    "Responda todos os itens antes de fechar o checklist."
            );
        }
        List<NaoConformidadeEntity> registros = naoConformidadeRepository
                .findAllByRespostaAuditoriaIdAndRespostaItemChecklistId(
                        auditoria.getId(),
                        checklist.getId()
                );
        Set<UUID> respostasComNc = registros.stream()
                .map(nc -> nc.getResposta().getId())
                .collect(Collectors.toSet());
        boolean semEnvio = respostas.values().stream()
                .anyMatch(resposta ->
                        resposta.getResultado() == ResultadoItem.NAO_CONFORME
                                && !respostasComNc.contains(resposta.getId())
                );
        if (semEnvio) {
            throw new InvalidRequestException(
                    "Envie todas as não conformidades para resolução antes de fechar."
            );
        }

        List<NaoConformidadeEntity> pendentes = registros.stream()
                .filter(this::naoResolvida)
                .toList();
        if (pendentes.isEmpty()) {
            return;
        }
        if (auditoria.getConclusaoExcepcionalAutorizadaPor() == null
                || auditoria.getConclusaoExcepcionalAutorizadaEm() == null) {
            throw new InvalidRequestException(
                    "Existem não conformidades não resolvidas. Um superior deve autorizar a conclusão excepcional."
            );
        }
        if (pendentes.stream().anyMatch(nc -> nc.getEscalonamentos().isEmpty())) {
            throw new InvalidRequestException(
                    "A conclusão excepcional exige escalonamento em todas as não conformidades pendentes."
            );
        }
    }

    private VersaoExecucaoChecklistResponseDTO salvarSnapshot(
            ChecklistEntity checklist,
            ParticipacaoPlanoEntity autor,
            TipoVersaoExecucaoChecklist tipo,
            String observacao
    ) {
        int numero = versaoRepository
                .findTopByChecklistIdOrderByNumeroDesc(checklist.getId())
                .map(versao -> versao.getNumero() + 1)
                .orElse(1);
        VersaoExecucaoChecklistEntity versao = VersaoExecucaoChecklistEntity
                .builder()
                .checklist(checklist)
                .autor(autor)
                .numero(numero)
                .tipo(tipo)
                .observacao(observacao)
                .criadoEm(LocalDateTime.now())
                .build();

        Map<UUID, RespostaAuditoriaEntity> respostas = respostas(checklist);
        Map<UUID, NaoConformidadeEntity> registros = naoConformidadeRepository
                .findAllByRespostaAuditoriaIdAndRespostaItemChecklistId(
                        checklist.getAuditoria().getId(),
                        checklist.getId()
                )
                .stream()
                .collect(Collectors.toMap(
                        nc -> nc.getResposta().getId(),
                        Function.identity()
                ));
        checklist.getItens().stream()
                .sorted(Comparator.comparingInt(ItemChecklistEntity::getOrdem))
                .map(item -> copiarItem(
                        versao,
                        item,
                        respostas.get(item.getId()),
                        registros
                ))
                .forEach(versao.getItens()::add);
        return toResponse(versaoRepository.save(versao));
    }

    private ItemVersaoExecucaoEntity copiarItem(
            VersaoExecucaoChecklistEntity versao,
            ItemChecklistEntity item,
            RespostaAuditoriaEntity resposta,
            Map<UUID, NaoConformidadeEntity> registros
    ) {
        NaoConformidadeEntity nc = resposta == null
                ? null
                : registros.get(resposta.getId());
        OffsetDateTime escalonadoEm = nc == null
                ? null
                : nc.getEscalonamentos().stream()
                        .map(EscalonamentoNcEntity::getEscalonadoEm)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
        return ItemVersaoExecucaoEntity.builder()
                .versao(versao)
                .ordem(item.getOrdem())
                .descricao(item.getPergunta())
                .resultado(resposta == null ? null : resposta.getResultado())
                .observacao(resposta == null ? null : resposta.getObservacao())
                .ncIdentificadaEm(dataIdentificacao(resposta, nc))
                .responsavelResolucao(nc == null
                        ? null
                        : nc.getResponsavel() == null
                                ? "Equipe de resolução"
                                : nc.getResponsavel().getUsuario().getNome())
                .responsavelParticipacaoId(nc == null || nc.getResponsavel() == null
                        ? null
                        : nc.getResponsavel().getId())
                .classificacaoNc(nc == null ? null : nc.getClassificacao())
                .acaoCorretiva(nc == null ? null : nc.getAcaoCorretiva())
                .prazoResolucaoEm(nc == null ? null : nc.getPrazoEm())
                .escalonadoEm(escalonadoEm)
                .ncConcluidaEm(nc == null ? null : nc.getConcluidaEm())
                .statusNc(nc == null ? null : nc.getStatus())
                .build();
    }

    private void atualizarDadosNaoConformidade(
            ItemVersaoExecucaoEntity item,
            UUID planoId,
            AtualizarItemVersaoExecucaoRequestDTO dto
    ) {
        OffsetDateTime identificadaEm = item.getNcIdentificadaEm();
        if (identificadaEm == null) {
            identificadaEm = OffsetDateTime.now(ZoneOffset.UTC);
            item.setNcIdentificadaEm(identificadaEm);
        }

        atualizarResponsavel(item, planoId, dto);
        boolean classificacaoAlterada = item.getClassificacaoNc()
                != dto.classificacaoNc();
        item.setClassificacaoNc(dto.classificacaoNc());
        item.setAcaoCorretiva(normalizar(dto.acaoCorretiva()));

        if (dto.classificacaoNc() == null) {
            item.setPrazoResolucaoEm(null);
        } else if (classificacaoAlterada || item.getPrazoResolucaoEm() == null) {
            int prazoHoras = configuracaoPlanoService
                    .buscarAtiva(planoId, dto.classificacaoNc())
                    .getPrazoHoras();
            item.setPrazoResolucaoEm(calendarioPrazoService.calcularPrazo(
                    planoId,
                    identificadaEm,
                    prazoHoras
            ));
        }
    }

    private void atualizarResponsavel(
            ItemVersaoExecucaoEntity item,
            UUID planoId,
            AtualizarItemVersaoExecucaoRequestDTO dto
    ) {
        if (dto.responsavelParticipacaoId() == null) {
            boolean nomeLegadoPreservado = item.getResponsavelParticipacaoId() == null
                    && item.getResponsavelResolucao() != null
                    && item.getResponsavelResolucao().equals(
                            normalizar(dto.responsavelResolucao())
                    );
            if (!nomeLegadoPreservado) {
                item.setResponsavelResolucao(null);
            }
            item.setResponsavelParticipacaoId(null);
            return;
        }
        ParticipacaoPlanoEntity responsavel = participacaoRepository
                .findByIdAndPlanoId(dto.responsavelParticipacaoId(), planoId)
                .filter(participacao -> participacao.possuiPapel(
                        PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
                ))
                .orElseThrow(() -> new InvalidRequestException(
                        "Selecione um membro ativo da equipe de resolução."
                ));
        item.setResponsavelParticipacaoId(responsavel.getId());
        item.setResponsavelResolucao(responsavel.getUsuario().getNome());
    }

    private void limparDadosNaoConformidade(ItemVersaoExecucaoEntity item) {
        item.setObservacao(null);
        item.setNcIdentificadaEm(null);
        item.setResponsavelParticipacaoId(null);
        item.setResponsavelResolucao(null);
        item.setClassificacaoNc(null);
        item.setAcaoCorretiva(null);
        item.setPrazoResolucaoEm(null);
        item.setEscalonadoEm(null);
        item.setNcConcluidaEm(null);
        item.setStatusNc(null);
    }

    private OffsetDateTime dataIdentificacao(
            RespostaAuditoriaEntity resposta,
            NaoConformidadeEntity naoConformidade
    ) {
        if (naoConformidade != null) {
            return naoConformidade.getIdentificadoEm();
        }
        if (resposta == null
                || resposta.getResultado() != ResultadoItem.NAO_CONFORME) {
            return null;
        }
        return resposta.getRespondidoEm()
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime();
    }

    private Map<UUID, RespostaAuditoriaEntity> respostas(
            ChecklistEntity checklist
    ) {
        return checklist.getAuditoria().getRespostas().stream()
                .filter(resposta ->
                        resposta.getItem().getChecklist().getId()
                                .equals(checklist.getId()))
                .collect(Collectors.toMap(
                        resposta -> resposta.getItem().getId(),
                        Function.identity()
                ));
    }

    private ContextoExecucao buscarParaAtualizacao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(auth, planoId, PermissaoPlano.AUDITAR);
        ChecklistEntity checklist = checklistRepository
                .buscarParaAtualizacao(
                        planoId, artefatoId, auditoriaId, checklistId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Checklist não encontrado."
                ));
        AuditoriaEntity auditoria = checklist.getAuditoria();
        if (!participacao.getId().equals(auditoria.getAuditor().getId())) {
            throw new AccessDeniedException(
                    "Somente o auditor designado pode executar este checklist."
            );
        }
        return new ContextoExecucao(auditoria, checklist, participacao);
    }

    private AuditoriaEntity buscarAuditoria(
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        return auditoriaRepository
                .findByIdAndArtefatoIdAndArtefatoDocumentoPlanoId(
                        auditoriaId, artefatoId, planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Auditoria não encontrada."
                ));
    }

    private ChecklistEntity buscarChecklist(
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId
    ) {
        return checklistRepository
                .findByIdAndAuditoriaIdAndAuditoriaArtefatoIdAndAuditoriaArtefatoDocumentoPlanoId(
                        checklistId, auditoriaId, artefatoId, planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Checklist não encontrado."
                ));
    }

    private boolean naoResolvida(NaoConformidadeEntity nc) {
        return !STATUS_FINAIS.contains(nc.getStatus());
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private VersaoExecucaoChecklistResponseDTO toResponse(
            VersaoExecucaoChecklistEntity versao
    ) {
        return new VersaoExecucaoChecklistResponseDTO(
                versao.getId(),
                versao.getChecklist().getId(),
                versao.getNumero(),
                versao.getTipo(),
                versao.getObservacao(),
                versao.getAutor().getUsuario().getNome(),
                versao.getCriadoEm(),
                versao.getItens().stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private ItemVersaoExecucaoResponseDTO toItemResponse(
            ItemVersaoExecucaoEntity item
    ) {
        return new ItemVersaoExecucaoResponseDTO(
                item.getId(),
                item.getOrdem(),
                item.getDescricao(),
                item.getResultado(),
                item.getObservacao(),
                item.getNcIdentificadaEm(),
                item.getResponsavelParticipacaoId(),
                item.getResponsavelResolucao(),
                item.getClassificacaoNc(),
                item.getAcaoCorretiva(),
                item.getPrazoResolucaoEm(),
                item.getEscalonadoEm(),
                item.getNcConcluidaEm(),
                item.getStatusNc()
        );
    }

    private record ContextoExecucao(
            AuditoriaEntity auditoria,
            ChecklistEntity checklist,
            ParticipacaoPlanoEntity participacao
    ) {
    }
}
