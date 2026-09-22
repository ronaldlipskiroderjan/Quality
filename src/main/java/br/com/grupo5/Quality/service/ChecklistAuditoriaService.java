package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusChecklist;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.ChecklistRepository;
import br.com.grupo5.Quality.database.repository.ItemChecklistRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.dto.request.ItemChecklistRequestDTO;
import br.com.grupo5.Quality.dto.response.ItemChecklistResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChecklistAuditoriaService {

    private final AuditoriaRepository auditoriaRepository;
    private final ChecklistRepository checklistRepository;
    private final ItemChecklistRepository itemRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public ChecklistEntity criarInicial(AuditoriaEntity auditoria) {
        boolean possuiChecklistAtivo = auditoria.getChecklists().stream()
                .anyMatch(checklist ->
                        checklist.getStatus() != StatusChecklist.ARQUIVADO);
        if (possuiChecklistAtivo) {
            throw new AlreadyExistsException(
                    "A auditoria já possui um checklist ativo."
            );
        }
        LocalDateTime agora = LocalDateTime.now();
        ChecklistEntity checklist = ChecklistEntity.builder()
                .auditoria(auditoria)
                .versao("1.0")
                .status(StatusChecklist.PUBLICADO)
                .criadoEm(agora)
                .atualizadoEm(agora)
                .build();
        checklist = checklistRepository.save(checklist);
        ItemChecklistEntity primeiraLinha = criarLinhaVazia(checklist, 1, agora);
        primeiraLinha = itemRepository.save(primeiraLinha);
        checklist.getItens().add(primeiraLinha);
        auditoria.getChecklists().add(checklist);
        return checklist;
    }

    @Transactional
    public ItemChecklistResponseDTO adicionarItem(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            ItemChecklistRequestDTO dto
    ) {
        ChecklistEntity checklist = buscarChecklistParaEdicao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        validarOrdemNova(checklistId, dto.ordem());

        ItemChecklistEntity item = ItemChecklistEntity.builder()
                .checklist(checklist)
                .ordem(dto.ordem())
                .pergunta(normalizarPergunta(dto.pergunta()))
                .origem(OrigemItemChecklist.MANUAL)
                .criadoEm(LocalDateTime.now())
                .build();
        item = itemRepository.save(item);
        checklist.getItens().add(item);
        ordenarItens(checklist);
        atualizarData(checklist);
        return toItem(item);
    }

    @Transactional
    public ItemChecklistResponseDTO atualizarItem(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            UUID itemId,
            ItemChecklistRequestDTO dto
    ) {
        ChecklistEntity checklist = buscarChecklistParaEdicao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        ItemChecklistEntity item = buscarItem(checklistId, itemId);
        if (itemRepository.existsByChecklistIdAndOrdemAndIdNot(
                checklistId, dto.ordem(), itemId
        )) {
            throw new AlreadyExistsException("Já existe um item nesta ordem.");
        }
        item.setOrdem(dto.ordem());
        String pergunta = normalizarPergunta(dto.pergunta());
        if (!checklist.rascunho() && pergunta.isBlank()) {
            throw new InvalidRequestException(
                    "A pergunta não pode ficar vazia durante a execução."
            );
        }
        item.setPergunta(pergunta);
        ordenarItens(checklist);
        atualizarData(checklist);
        return toItem(itemRepository.save(item));
    }

    @Transactional
    public void removerItem(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId,
            UUID itemId
    ) {
        ChecklistEntity checklist = buscarChecklistParaEdicao(
                auth, planoId, artefatoId, auditoriaId, checklistId
        );
        AuditoriaEntity auditoria = checklist.getAuditoria();
        ItemChecklistEntity item = buscarItem(checklistId, itemId);
        int ordemRemovida = item.getOrdem();

        RespostaAuditoriaEntity resposta = auditoria.getRespostas().stream()
                .filter(registro -> registro.getItem().getId().equals(itemId))
                .findFirst()
                .orElse(null);
        if (resposta != null) {
            NaoConformidadeEntity naoConformidade =
                    naoConformidadeRepository.findByRespostaId(resposta.getId())
                            .orElse(null);
            if (naoConformidade != null && !naoConformidade.rascunho()) {
                throw new InvalidRequestException(
                        "A pergunta possui uma não conformidade já enviada e deve permanecer no histórico da auditoria."
                );
            }
            if (naoConformidade != null) {
                naoConformidadeRepository.delete(naoConformidade);
                naoConformidadeRepository.flush();
            }
            auditoria.getRespostas().remove(resposta);
            atualizarTotais(auditoria);
            auditoriaRepository.saveAndFlush(auditoria);
        }

        checklist.getItens().remove(item);
        atualizarData(checklist);
        checklistRepository.saveAndFlush(checklist);
        itemRepository.compactarOrdensApos(checklistId, ordemRemovida);
    }

    private AuditoriaEntity buscarParaEdicao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId
    ) {
        ParticipacaoPlanoEntity participacao = acessoPlanoService
                .buscarParticipacao(auth, planoId, PermissaoPlano.AUDITAR);
        AuditoriaEntity auditoria = buscarAuditoria(
                planoId, artefatoId, auditoriaId
        );
        if (!participacao.getId().equals(auditoria.getAuditor().getId())) {
            throw new AccessDeniedException(
                    "Somente o auditor designado pode preparar esta auditoria."
            );
        }
        if (auditoria.getStatus() != StatusAuditoria.EM_PREPARACAO
                && auditoria.getStatus() != StatusAuditoria.EM_ANDAMENTO) {
            throw new InvalidRequestException(
                    "A auditoria não permite alterar o checklist."
            );
        }
        return auditoria;
    }

    private ChecklistEntity buscarChecklistParaEdicao(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            UUID auditoriaId,
            UUID checklistId
    ) {
        buscarParaEdicao(auth, planoId, artefatoId, auditoriaId);
        ChecklistEntity checklist = buscarChecklist(
                planoId, artefatoId, auditoriaId, checklistId
        );
        if (checklist.getStatus() != StatusChecklist.RASCUNHO
                && checklist.getStatus() != StatusChecklist.PUBLICADO) {
            throw new InvalidRequestException(
                    "Somente checklists em preparação ou execução podem ser alterados."
            );
        }
        return checklist;
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

    private ItemChecklistEntity buscarItem(UUID checklistId, UUID itemId) {
        return itemRepository.findByIdAndChecklistId(itemId, checklistId)
                .orElseThrow(() -> new NotFoundException(
                        "Item do checklist não encontrado."
                ));
    }

    private void validarOrdemNova(UUID checklistId, int ordem) {
        if (itemRepository.existsByChecklistIdAndOrdem(checklistId, ordem)) {
            throw new AlreadyExistsException("Já existe um item nesta ordem.");
        }
    }

    private void ordenarItens(ChecklistEntity checklist) {
        checklist.getItens().sort(Comparator.comparingInt(
                ItemChecklistEntity::getOrdem
        ));
    }

    private ItemChecklistEntity criarLinhaVazia(
            ChecklistEntity checklist,
            int ordem,
            LocalDateTime criadoEm
    ) {
        return ItemChecklistEntity.builder()
                .checklist(checklist)
                .ordem(ordem)
                .pergunta("")
                .origem(OrigemItemChecklist.MANUAL)
                .criadoEm(criadoEm)
                .build();
    }

    private void atualizarData(ChecklistEntity checklist) {
        checklist.setAtualizadoEm(LocalDateTime.now());
    }

    private void atualizarTotais(AuditoriaEntity auditoria) {
        int conformes = contar(auditoria, ResultadoItem.CONFORME);
        int naoConformes = contar(auditoria, ResultadoItem.NAO_CONFORME);
        int naoAplicaveis = contar(auditoria, ResultadoItem.NAO_APLICAVEL);
        int aplicaveis = conformes + naoConformes;

        auditoria.setConformes(conformes);
        auditoria.setNaoConformes(naoConformes);
        auditoria.setNaoAplicaveis(naoAplicaveis);
        auditoria.setAderenciaPercentual(aplicaveis == 0
                ? null
                : BigDecimal.valueOf(conformes)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                BigDecimal.valueOf(aplicaveis),
                                2,
                                RoundingMode.HALF_UP
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

    private String normalizarPergunta(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private ItemChecklistResponseDTO toItem(ItemChecklistEntity item) {
        return new ItemChecklistResponseDTO(
                item.getId(),
                item.getOrdem(),
                item.getPergunta(),
                item.getOrigem(),
                item.getCriadoEm()
        );
    }
}
