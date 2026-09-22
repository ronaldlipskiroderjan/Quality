package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusChecklist;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.ArtefatoRepository;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.ItemChecklistRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.database.repository.RespostaAuditoriaRepository;
import br.com.grupo5.Quality.dto.request.RespostaAuditoriaRequestDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaDetalhadaResponseDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.ItemAuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoResponseDTO;
import br.com.grupo5.Quality.dto.response.ChecklistAuditoriaDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.ChecklistResponseDTO;
import org.springframework.data.domain.Pageable;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Set<StatusNaoConformidade> STATUS_NC_FINAIS = Set.of(
            StatusNaoConformidade.CONCLUIDA,
            StatusNaoConformidade.CANCELADA
    );

    private final AuditoriaRepository auditoriaRepository;
    private final RespostaAuditoriaRepository respostaRepository;
    private final ArtefatoRepository artefatoRepository;
    private final ItemChecklistRepository itemRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional(readOnly = true)
    public PaginaResponseDTO<AuditoriaResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        buscarArtefato(planoId, artefatoId);
        return PaginaResponseDTO.de(auditoriaRepository
                .findAllByArtefatoId(artefatoId, pageable)
                .map(this::toResumo));
    }

    @Transactional(readOnly = true)
    public AuditoriaDetalhadaResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return toDetalhada(buscarAuditoria(
                planoId,
                artefatoId,
                auditoriaId
        ));
    }

    @Transactional
    public ItemAuditoriaResponseDTO responder(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID itemId,
            RespostaAuditoriaRequestDTO dto
    ) {
        AuditoriaEntity auditoria = buscarAuditoriaParaExecucao(
                auth,
                planoId,
                artefatoId,
                auditoriaId
        );
        validarEmAndamento(auditoria);
        ItemChecklistEntity item = itemRepository
                .findByIdAndChecklistAuditoriaId(itemId, auditoriaId)
                .orElseThrow(() -> new NotFoundException(
                        "Item do checklist não encontrado."
                ));
        if (item.getChecklist().getStatus() != StatusChecklist.PUBLICADO) {
            throw new InvalidRequestException(
                    "O checklist não está disponível para respostas."
            );
        }
        if (item.getPergunta().isBlank()) {
            throw new InvalidRequestException(
                    "Preencha a pergunta antes de responder o item."
            );
        }
        String observacao = limparObservacao(dto.observacao());

        LocalDateTime agora = LocalDateTime.now();
        RespostaAuditoriaEntity resposta = respostaRepository
                .findByAuditoriaIdAndItemId(auditoriaId, itemId)
                .orElse(null);
        validarMudancaResposta(resposta, dto.resultado());

        if (resposta == null) {
            resposta = RespostaAuditoriaEntity.builder()
                    .auditoria(auditoria)
                    .item(item)
                    .respondidoEm(agora)
                    .build();
        }

        resposta.setResultado(dto.resultado());
        resposta.setObservacao(observacao);
        resposta.setAtualizadoEm(agora);
        boolean novaResposta = resposta.getId() == null;
        resposta = respostaRepository.save(resposta);

        if (novaResposta) {
            auditoria.getRespostas().add(resposta);
        }

        atualizarTotais(auditoria);
        auditoriaRepository.save(auditoria);
        return toItem(item, resposta);
    }

    @Transactional
    public void marcarConformeAposResolucao(
            NaoConformidadeEntity naoConformidade
    ) {
        RespostaAuditoriaEntity resposta = naoConformidade.getResposta();
        if (resposta.getResultado() == ResultadoItem.CONFORME) {
            return;
        }

        resposta.setResultado(ResultadoItem.CONFORME);
        resposta.setObservacao(null);
        resposta.setAtualizadoEm(LocalDateTime.now());
        respostaRepository.save(resposta);

        AuditoriaEntity auditoria = resposta.getAuditoria();
        atualizarTotais(auditoria);
        auditoriaRepository.save(auditoria);
    }

    @Transactional
    public AuditoriaDetalhadaResponseDTO concluir(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        AuditoriaEntity auditoria = buscarAuditoriaParaExecucao(
                auth,
                planoId,
                artefatoId,
                auditoriaId
        );
        validarEmAndamento(auditoria);

        List<ChecklistEntity> checklists = checklistsAtivos(auditoria);
        if (checklists.isEmpty() || checklists.stream().anyMatch(checklist ->
                checklist.getStatus() != StatusChecklist.PUBLICADO)) {
            throw new InvalidRequestException(
                    "O checklist da auditoria não está disponível."
            );
        }

        if (auditoria.getRespostas().size() != itensAtivos(auditoria).size()) {
            throw new InvalidRequestException(
                    "Todos os itens devem ser respondidos antes da conclusão."
            );
        }

        atualizarTotais(auditoria);
        List<NaoConformidadeEntity> naoConformidades =
                naoConformidadeRepository.findAllByRespostaAuditoriaId(
                        auditoriaId
                );
        Set<UUID> respostasComNcEnviada = naoConformidades.stream()
                .filter(naoConformidade -> !naoConformidade.rascunho())
                .map(naoConformidade ->
                        naoConformidade.getResposta().getId())
                .collect(Collectors.toSet());
        boolean existeNaoConformeSemEnvio = auditoria.getRespostas().stream()
                .filter(resposta ->
                        resposta.getResultado() == ResultadoItem.NAO_CONFORME)
                .anyMatch(resposta ->
                        !respostasComNcEnviada.contains(resposta.getId()));
        if (existeNaoConformeSemEnvio) {
            throw new InvalidRequestException(
                    "Todo item não conforme deve ser enviado para resolução."
            );
        }

        validarPendenciasParaConclusao(auditoria, naoConformidades);
        checklists.stream()
                .filter(checklist ->
                        checklist.getStatus() == StatusChecklist.PUBLICADO)
                .forEach(checklist -> {
                    checklist.setStatus(StatusChecklist.CONCLUIDO);
                    checklist.setAtualizadoEm(LocalDateTime.now());
                });
        auditoria.setStatus(StatusAuditoria.CONCLUIDA);
        auditoria.setDataFim(LocalDateTime.now());
        auditoria.getArtefato().setStatus(StatusArtefato.CONCLUIDO);
        return toDetalhada(auditoriaRepository.save(auditoria));
    }

    private void validarPendenciasParaConclusao(
            AuditoriaEntity auditoria,
            List<NaoConformidadeEntity> naoConformidades
    ) {
        List<NaoConformidadeEntity> pendentes = naoConformidades.stream()
                .filter(naoConformidade ->
                        !STATUS_NC_FINAIS.contains(naoConformidade.getStatus()))
                .toList();
        if (pendentes.isEmpty()) {
            return;
        }
        if (auditoria.getConclusaoExcepcionalAutorizadaPor() == null
                || auditoria.getConclusaoExcepcionalAutorizadaEm() == null) {
            throw new InvalidRequestException(
                    "Existem não conformidades pendentes. Um superior deve autorizar a conclusão excepcional."
            );
        }
        if (pendentes.stream().anyMatch(naoConformidade ->
                naoConformidade.getEscalonamentos().isEmpty())) {
            throw new InvalidRequestException(
                    "A conclusão excepcional exige escalonamento em todas as não conformidades pendentes."
            );
        }
    }

    private AuditoriaEntity buscarAuditoriaParaExecucao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(auth, planoId, PermissaoPlano.AUDITAR);
        AuditoriaEntity auditoria = buscarAuditoria(
                planoId,
                artefatoId,
                auditoriaId
        );
        validarAuditorAtribuido(participacao, auditoria.getAuditor());
        return auditoria;
    }

    private void validarAuditorAtribuido(
            ParticipacaoPlanoEntity participacao,
            ParticipacaoPlanoEntity auditorAtribuido
    ) {
        if (auditorAtribuido == null
                || !auditorAtribuido.getId().equals(participacao.getId())) {
            throw new AccessDeniedException(
                    "Somente o auditor atribuído pode executar esta auditoria."
            );
        }
    }

    private ArtefatoEntity buscarArtefato(UUID planoId, UUID artefatoId) {
        return artefatoRepository
                .findByIdAndDocumentoPlanoId(artefatoId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Artefato não encontrado."
                ));
    }

    private AuditoriaEntity buscarAuditoria(
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        return auditoriaRepository
                .findByIdAndArtefatoIdAndArtefatoDocumentoPlanoId(
                        auditoriaId,
                        artefatoId,
                        planoId
                )
                .orElseThrow(() -> new NotFoundException(
                        "Auditoria não encontrada."
                ));
    }

    private void validarEmAndamento(AuditoriaEntity auditoria) {
        if (!auditoria.emAndamento()) {
            throw new InvalidRequestException(
                    "Somente auditorias em andamento podem ser alteradas."
            );
        }
    }

    private void validarMudancaResposta(
            RespostaAuditoriaEntity resposta,
            ResultadoItem novoResultado
    ) {
        if (resposta != null
                && resposta.getResultado() == ResultadoItem.NAO_CONFORME
                && novoResultado != ResultadoItem.NAO_CONFORME) {
            naoConformidadeRepository.findByRespostaId(resposta.getId())
                    .ifPresent(naoConformidade -> {
                        if (!naoConformidade.rascunho()) {
                            if (!STATUS_NC_FINAIS.contains(
                                    naoConformidade.getStatus())) {
                                throw new InvalidRequestException(
                                        "O resultado não pode ser alterado enquanto a não conformidade estiver em tratamento."
                                );
                            }
                            return;
                        }
                        naoConformidadeRepository.delete(naoConformidade);
                    });
        }
    }

    private String limparObservacao(String observacao) {
        if (observacao == null || observacao.isBlank()) {
            return null;
        }
        return observacao.trim();
    }

    private void atualizarTotais(AuditoriaEntity auditoria) {
        int conformes = contar(auditoria, ResultadoItem.CONFORME);
        int naoConformes = contar(auditoria, ResultadoItem.NAO_CONFORME);
        int naoAplicaveis = contar(auditoria, ResultadoItem.NAO_APLICAVEL);
        auditoria.setConformes(conformes);
        auditoria.setNaoConformes(naoConformes);
        auditoria.setNaoAplicaveis(naoAplicaveis);
        auditoria.setAderenciaPercentual(calcularAderencia(
                conformes,
                naoConformes
        ));
    }

    private int contar(
            AuditoriaEntity auditoria,
            ResultadoItem resultado
    ) {
        return (int) auditoria.getRespostas().stream()
                .filter(resposta -> resposta.getResultado() == resultado)
                .count();
    }

    private BigDecimal calcularAderencia(int conformes, int naoConformes) {
        int aplicaveis = conformes + naoConformes;
        if (aplicaveis == 0) {
            return null;
        }

        return BigDecimal.valueOf(conformes)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        BigDecimal.valueOf(aplicaveis),
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private AuditoriaResponseDTO toResumo(AuditoriaEntity auditoria) {
        List<ChecklistEntity> checklists = checklistsAtivos(auditoria);
        return new AuditoriaResponseDTO(
                auditoria.getId(),
                auditoria.getArtefato().getId(),
                auditoria.getAuditor().getId(),
                auditoria.getAuditor().getUsuario().getNome(),
                auditoria.getStatus(),
                checklists.size(),
                checklists.stream().mapToInt(item -> item.getItens().size()).sum(),
                auditoria.getRespostas().size(),
                auditoria.getConformes(),
                auditoria.getNaoConformes(),
                auditoria.getNaoAplicaveis(),
                auditoria.getAderenciaPercentual(),
                documentosReferencia(auditoria),
                checklists.stream().map(this::toChecklistResumo).toList(),
                auditoria.getDataInicio(),
                auditoria.getDataFim(),
                auditoria.getVersaoRegistro()
        );
    }

    private AuditoriaDetalhadaResponseDTO toDetalhada(
            AuditoriaEntity auditoria
    ) {
        Map<UUID, RespostaAuditoriaEntity> respostas = auditoria
                .getRespostas()
                .stream()
                .collect(Collectors.toMap(
                        resposta -> resposta.getItem().getId(),
                        Function.identity()
                ));

        List<ChecklistEntity> checklists = checklistsAtivos(auditoria);

        return new AuditoriaDetalhadaResponseDTO(
                auditoria.getId(),
                auditoria.getArtefato().getId(),
                auditoria.getAuditor().getId(),
                auditoria.getAuditor().getUsuario().getNome(),
                auditoria.getStatus(),
                checklists.size(),
                checklists.stream().mapToInt(item -> item.getItens().size()).sum(),
                auditoria.getRespostas().size(),
                auditoria.getConformes(),
                auditoria.getNaoConformes(),
                auditoria.getNaoAplicaveis(),
                auditoria.getAderenciaPercentual(),
                documentosReferencia(auditoria),
                checklists.stream()
                        .map(checklist -> toChecklistDetalhado(checklist, respostas))
                        .toList(),
                auditoria.getDataInicio(),
                auditoria.getDataFim(),
                auditoria.getConclusaoExcepcionalAutorizadaPor() == null
                        ? null
                        : auditoria.getConclusaoExcepcionalAutorizadaPor().getId(),
                auditoria.getConclusaoExcepcionalAutorizadaPor() == null
                        ? null
                        : auditoria.getConclusaoExcepcionalAutorizadaPor()
                                .getUsuario().getNome(),
                auditoria.getConclusaoExcepcionalAutorizadaEm(),
                auditoria.getJustificativaConclusaoExcepcional(),
                auditoria.getVersaoRegistro()
        );
    }

    private List<ChecklistEntity> checklistsAtivos(AuditoriaEntity auditoria) {
        return auditoria.getChecklists().stream()
                .filter(checklist -> checklist.getStatus() != StatusChecklist.ARQUIVADO)
                .sorted(Comparator.comparing(ChecklistEntity::getCriadoEm))
                .toList();
    }

    private List<ItemChecklistEntity> itensAtivos(AuditoriaEntity auditoria) {
        return checklistsAtivos(auditoria).stream()
                .flatMap(checklist -> checklist.getItens().stream())
                .toList();
    }

    private ChecklistResponseDTO toChecklistResumo(ChecklistEntity checklist) {
        return new ChecklistResponseDTO(
                checklist.getId(),
                checklist.getAuditoria().getId(),
                checklist.getAuditoria().getArtefato().getId(),
                checklist.getVersao(),
                checklist.getStatus(),
                checklist.getItens().size(),
                checklist.getCriadoEm(),
                checklist.getAtualizadoEm(),
                checklist.getVersaoRegistro()
        );
    }

    private ChecklistAuditoriaDetalhadoResponseDTO toChecklistDetalhado(
            ChecklistEntity checklist,
            Map<UUID, RespostaAuditoriaEntity> respostas
    ) {
        List<ItemAuditoriaResponseDTO> itens = checklist.getItens().stream()
                .sorted(Comparator.comparingInt(ItemChecklistEntity::getOrdem))
                .map(item -> toItem(item, respostas.get(item.getId())))
                .toList();
        return new ChecklistAuditoriaDetalhadoResponseDTO(
                checklist.getId(),
                checklist.getAuditoria().getId(),
                checklist.getAuditoria().getArtefato().getId(),
                checklist.getVersao(),
                checklist.getStatus(),
                itens.size(),
                itens,
                checklist.getCriadoEm(),
                checklist.getAtualizadoEm(),
                checklist.getVersaoRegistro()
        );
    }

    private List<DocumentoResponseDTO> documentosReferencia(
            AuditoriaEntity auditoria
    ) {
        return auditoria.getDocumentosReferencia().stream()
                .map(documento -> new DocumentoResponseDTO(
                        documento.getId(),
                        documento.getNome(),
                        documento.getNomeArquivo(),
                        documento.getVersao(),
                        documento.getTipoArquivo(),
                        documento.getTamanho(),
                        documento.getClassificacao()
                ))
                .toList();
    }

    private ItemAuditoriaResponseDTO toItem(
            ItemChecklistEntity item,
            RespostaAuditoriaEntity resposta
    ) {
        if (resposta == null) {
            return new ItemAuditoriaResponseDTO(
                    item.getId(),
                    null,
                    item.getOrdem(),
                    item.getPergunta(),
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new ItemAuditoriaResponseDTO(
                item.getId(),
                resposta.getId(),
                item.getOrdem(),
                item.getPergunta(),
                resposta.getResultado(),
                resposta.getObservacao(),
                resposta.getRespondidoEm(),
                resposta.getAtualizadoEm(),
                resposta.getVersaoRegistro()
        );
    }
}
