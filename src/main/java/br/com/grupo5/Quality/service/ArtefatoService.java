package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.Classificacao;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.repository.ArtefatoRepository;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.DocumentoRepository;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.request.ArtefatoRequestDTO;
import br.com.grupo5.Quality.dto.response.ArtefatoResponseDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaAgendaResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtefatoService {

    private final ArtefatoRepository artefatoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final DocumentoRepository documentoRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final AcessoPlanoService acessoPlanoService;
    private final ChecklistAuditoriaService checklistService;

    @Transactional
    public ArtefatoResponseDTO criar(
            Authentication auth,
            UUID planoId,
            ArtefatoRequestDTO dto
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_ARTEFATOS
        );
        DocumentoEntity documento = buscarDocumento(planoId, dto.documentoId());
        ParticipacaoPlanoEntity auditor = buscarAuditor(
                planoId,
                dto.auditorParticipacaoId()
        );
        String nome = dto.nome().trim();
        String versao = dto.versao().trim();
        validarDuplicidade(documento.getId(), nome, versao);
        LinkedHashSet<DocumentoEntity> referencias = buscarReferencias(
                planoId,
                dto.documentoReferenciaIds()
        );

        LocalDateTime agora = LocalDateTime.now();
        ArtefatoEntity artefato = ArtefatoEntity.builder()
                .documento(documento)
                .auditor(auditor)
                .nome(nome)
                .versao(versao)
                .dataPlanejada(dto.dataPlanejada())
                .status(StatusArtefato.EM_ANDAMENTO)
                .criadoEm(agora)
                .build();

        artefato = artefatoRepository.save(artefato);
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .artefato(artefato)
                .auditor(auditor)
                .documentosReferencia(referencias)
                .dataInicio(agora)
                .status(StatusAuditoria.EM_ANDAMENTO)
                .build();
        auditoria = auditoriaRepository.save(auditoria);
        artefato.setAuditoria(auditoria);
        checklistService.criarInicial(auditoria);
        documento.getArtefatos().add(artefato);
        auditor.getArtefatosAuditados().add(artefato);
        return toResponse(artefato);
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<ArtefatoResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return PaginaResponseDTO.de(artefatoRepository
                .findAllByDocumentoPlanoId(planoId, pageable)
                .map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<AuditoriaAgendaResponseDTO> listarAgenda(
            Authentication auth,
            Pageable pageable
    ) {
        return PaginaResponseDTO.de(artefatoRepository
                .findAllByAuditorUsuarioEmailIgnoreCaseAndAuditorAtivoTrue(
                        auth.getName(),
                        pageable
                )
                .map(this::toAgendaResponse));
    }

    @Transactional(readOnly = true)
    public ArtefatoResponseDTO buscar(
            Authentication auth,
            UUID planoId,
            UUID artefatoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return toResponse(buscarArtefato(planoId, artefatoId));
    }

    @Transactional
    public ArtefatoResponseDTO atualizar(
            Authentication auth,
            UUID planoId,
            UUID artefatoId,
            ArtefatoRequestDTO dto
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_ARTEFATOS
        );
        ArtefatoEntity artefato = buscarArtefato(planoId, artefatoId);
        DocumentoEntity documento = buscarDocumento(planoId, dto.documentoId());
        ParticipacaoPlanoEntity auditor = buscarAuditor(
                planoId,
                dto.auditorParticipacaoId()
        );
        String nome = dto.nome().trim();
        String versao = dto.versao().trim();
        LinkedHashSet<DocumentoEntity> referencias = buscarReferencias(
                planoId,
                dto.documentoReferenciaIds()
        );

        if (artefatoRepository
                .existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCaseAndIdNot(
                        documento.getId(),
                        nome,
                        versao,
                        artefatoId
                )) {
            throw new AlreadyExistsException(
                    "Já existe um artefato com este nome e versão no documento."
            );
        }

        atualizarAssociacoes(artefato, documento, auditor);
        artefato.setNome(nome);
        artefato.setVersao(versao);
        artefato.setDataPlanejada(dto.dataPlanejada());
        if (artefato.getAuditoria().getStatus() != StatusAuditoria.EM_PREPARACAO
                && artefato.getAuditoria().getStatus()
                        != StatusAuditoria.EM_ANDAMENTO) {
            throw new InvalidRequestException(
                    "As referências só podem ser alteradas durante a preparação."
            );
        }
        artefato.getAuditoria().setDocumentosReferencia(referencias);

        return toResponse(artefatoRepository.save(artefato));
    }

    @Transactional
    public void remover(
            Authentication auth,
            UUID planoId,
            UUID artefatoId
    ) {
        acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.GERENCIAR_ARTEFATOS
        );
        buscarArtefato(planoId, artefatoId);
        artefatoRepository.excluirComDependencias(artefatoId);
    }

    private DocumentoEntity buscarDocumento(UUID planoId, UUID documentoId) {
        DocumentoEntity documento = documentoRepository
                .findByIdAndPlanoId(documentoId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Documento não encontrado."
                ));
        if (documento.getClassificacao() != Classificacao.AUDITADO) {
            throw new InvalidRequestException(
                    "O artefato deve estar associado a um documento auditado."
            );
        }
        return documento;
    }

    private LinkedHashSet<DocumentoEntity> buscarReferencias(
            UUID planoId,
            Set<UUID> documentoIds
    ) {
        return documentoIds.stream()
                .map(documentoId -> documentoRepository
                        .findByIdAndPlanoId(documentoId, planoId)
                        .orElseThrow(() -> new NotFoundException(
                                "Documento de referência não encontrado."
                        )))
                .peek(documento -> {
                    if (documento.getClassificacao() != Classificacao.REFERENCIA) {
                        throw new InvalidRequestException(
                                "Somente documentos de referência podem apoiar a auditoria."
                        );
                    }
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ParticipacaoPlanoEntity buscarAuditor(
            UUID planoId,
            UUID participanteId
    ) {
        ParticipacaoPlanoEntity auditor = participacaoRepository
                .findByIdAndPlanoId(participanteId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Auditor não encontrado no plano."
                ));
        if (!auditor.possuiPapel(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)) {
            throw new InvalidRequestException(
                    "O participante selecionado não é auditor e responsável de qualidade."
            );
        }
        return auditor;
    }

    private ArtefatoEntity buscarArtefato(UUID planoId, UUID artefatoId) {
        return artefatoRepository
                .findByIdAndDocumentoPlanoId(artefatoId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Artefato não encontrado."
                ));
    }

    private void validarDuplicidade(
            UUID documentoId,
            String nome,
            String versao
    ) {
        if (artefatoRepository
                .existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCase(
                        documentoId,
                        nome,
                        versao
                )) {
            throw new AlreadyExistsException(
                    "Já existe um artefato com este nome e versão no documento."
            );
        }
    }

    private void atualizarAssociacoes(
            ArtefatoEntity artefato,
            DocumentoEntity documento,
            ParticipacaoPlanoEntity auditor
    ) {
        if (!artefato.getDocumento().equals(documento)) {
            artefato.getDocumento().getArtefatos().remove(artefato);
            documento.getArtefatos().add(artefato);
            artefato.setDocumento(documento);
        }
        if (!artefato.getAuditor().equals(auditor)) {
            artefato.getAuditor().getArtefatosAuditados().remove(artefato);
            auditor.getArtefatosAuditados().add(artefato);
            artefato.setAuditor(auditor);
            artefato.getAuditoria().setAuditor(auditor);
        }
    }

    private ArtefatoResponseDTO toResponse(ArtefatoEntity artefato) {
        ParticipacaoPlanoEntity auditor = artefato.getAuditor();
        AuditoriaEntity auditoria = artefato.getAuditoria();
        return new ArtefatoResponseDTO(
                artefato.getId(),
                artefato.getDocumento().getId(),
                artefato.getDocumento().getNome(),
                artefato.getNome(),
                artefato.getVersao(),
                artefato.getDataPlanejada(),
                artefato.getStatus(),
                auditor.getId(),
                auditor.getUsuario().getId(),
                auditor.getUsuario().getNome(),
                auditor.getUsuario().getEmail(),
                auditoria.getId(),
                auditoria.getDocumentosReferencia().stream()
                        .map(this::toDocumento)
                        .toList(),
                (int) auditoria.getChecklists().stream()
                        .filter(checklist -> checklist.getStatus()
                                != br.com.grupo5.Quality.database.enums.StatusChecklist.ARQUIVADO)
                        .count(),
                artefato.getCriadoEm()
        );
    }

    private AuditoriaAgendaResponseDTO toAgendaResponse(
            ArtefatoEntity artefato
    ) {
        return new AuditoriaAgendaResponseDTO(
                artefato.getId(),
                artefato.getAuditoria().getId(),
                artefato.getDocumento().getPlano().getId(),
                artefato.getDocumento().getPlano().getNomeProjeto(),
                artefato.getNome(),
                artefato.getVersao(),
                artefato.getDataPlanejada(),
                artefato.getStatus(),
                artefato.getAuditor().getUsuario().getNome()
        );
    }

    private DocumentoResponseDTO toDocumento(DocumentoEntity documento) {
        return new DocumentoResponseDTO(
                documento.getId(),
                documento.getNome(),
                documento.getNomeArquivo(),
                documento.getVersao(),
                documento.getTipoArquivo(),
                documento.getTamanho(),
                documento.getClassificacao()
        );
    }
}
