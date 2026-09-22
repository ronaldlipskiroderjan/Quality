package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.Classificacao;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusChecklist;
import br.com.grupo5.Quality.database.repository.ArtefatoRepository;
import br.com.grupo5.Quality.database.repository.AuditoriaRepository;
import br.com.grupo5.Quality.database.repository.DocumentoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.request.ArtefatoRequestDTO;
import br.com.grupo5.Quality.dto.response.ArtefatoResponseDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaAgendaResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtefatoServiceTest {

    private static final UUID REFERENCIA_ID = UUID.fromString(
            "00000000-0000-0000-0000-000000000123"
    );

    @Mock
    private ArtefatoRepository artefatoRepository;
    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private DocumentoRepository documentoRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private ChecklistAuditoriaService checklistService;
    @Mock
    private Authentication auth;

    @Test
    void deveCriarArtefatoPlanejado() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano, Classificacao.AUDITADO);
        ParticipacaoPlanoEntity auditor = auditor(plano);

        autorizarGestao(plano);
        when(documentoRepository.findByIdAndPlanoId(
                documento.getId(),
                plano.getId()
        )).thenReturn(Optional.of(documento));
        when(participacaoRepository.findByIdAndPlanoId(
                auditor.getId(),
                plano.getId()
        )).thenReturn(Optional.of(auditor));
        when(artefatoRepository
                .existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCase(
                        documento.getId(),
                        "Documento de requisitos",
                        "1.0"
                )).thenReturn(false);
        when(artefatoRepository.save(any(ArtefatoEntity.class)))
                .thenAnswer(invocation -> {
                    ArtefatoEntity artefato = invocation.getArgument(0);
                    artefato.setId(UUID.randomUUID());
                    return artefato;
                });
        when(auditoriaRepository.save(any(AuditoriaEntity.class)))
                .thenAnswer(invocation -> {
                    AuditoriaEntity auditoria = invocation.getArgument(0);
                    auditoria.setId(UUID.randomUUID());
                    return auditoria;
                });
        when(checklistService.criarInicial(any(AuditoriaEntity.class)))
                .thenAnswer(invocation -> {
                    AuditoriaEntity auditoria = invocation.getArgument(0);
                    ChecklistEntity checklist = ChecklistEntity.builder()
                            .id(UUID.randomUUID())
                            .auditoria(auditoria)
                            .versao("1.0")
                            .status(StatusChecklist.PUBLICADO)
                            .criadoEm(LocalDateTime.now())
                            .atualizadoEm(LocalDateTime.now())
                            .build();
                    auditoria.getChecklists().add(checklist);
                    return checklist;
                });

        ArtefatoResponseDTO response = service().criar(
                auth,
                plano.getId(),
                dto(documento, auditor)
        );

        assertEquals(StatusArtefato.EM_ANDAMENTO, response.status());
        assertEquals(documento.getId(), response.documentoId());
        assertEquals(auditor.getId(), response.auditorParticipacaoId());
        assertEquals("Documento de requisitos", response.nome());
        assertEquals(1, response.documentosReferencia().size());
        assertEquals(1, response.totalChecklists());
        verify(checklistService).criarInicial(any(AuditoriaEntity.class));
        assertTrue(documento.getArtefatos().stream()
                .anyMatch(item -> item.getId().equals(response.id())));
    }

    @Test
    void naoDeveCriarArtefatoComDocumentoDeReferencia() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano, Classificacao.REFERENCIA);
        ParticipacaoPlanoEntity auditor = auditor(plano);

        autorizarGestao(plano);
        when(documentoRepository.findByIdAndPlanoId(
                documento.getId(),
                plano.getId()
        )).thenReturn(Optional.of(documento));

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        plano.getId(),
                        dto(documento, auditor)
                )
        );
        verify(artefatoRepository, never()).save(any());
    }

    @Test
    void naoDeveSelecionarParticipanteSemPapelDeAuditor() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano, Classificacao.AUDITADO);
        ParticipacaoPlanoEntity participante = participacao(
                plano,
                PapelPlano.SUPERIOR_N1
        );

        autorizarGestao(plano);
        when(documentoRepository.findByIdAndPlanoId(
                documento.getId(),
                plano.getId()
        )).thenReturn(Optional.of(documento));
        when(participacaoRepository.findByIdAndPlanoId(
                participante.getId(),
                plano.getId()
        )).thenReturn(Optional.of(participante));

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        plano.getId(),
                        dto(documento, participante)
                )
        );
        verify(artefatoRepository, never()).save(any());
    }

    @Test
    void naoDeveDuplicarNomeEVersaoNoMesmoDocumento() {
        PlanoEntity plano = plano();
        DocumentoEntity documento = documento(plano, Classificacao.AUDITADO);
        ParticipacaoPlanoEntity auditor = auditor(plano);

        autorizarGestao(plano);
        when(documentoRepository.findByIdAndPlanoId(
                documento.getId(),
                plano.getId()
        )).thenReturn(Optional.of(documento));
        when(participacaoRepository.findByIdAndPlanoId(
                auditor.getId(),
                plano.getId()
        )).thenReturn(Optional.of(auditor));
        when(artefatoRepository
                .existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCase(
                        documento.getId(),
                        "Documento de requisitos",
                        "1.0"
                )).thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> service().criar(
                        auth,
                        plano.getId(),
                        dto(documento, auditor)
                )
        );
        verify(artefatoRepository, never()).save(any());
    }

    @Test
    void deveListarArtefatosDoPlano() {
        PlanoEntity plano = plano();
        ArtefatoEntity artefato = artefato(
                documento(plano, Classificacao.AUDITADO),
                auditor(plano)
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId()))
                .thenReturn(plano);
        PageRequest pageable = PageRequest.of(0, 15);
        when(artefatoRepository.findAllByDocumentoPlanoId(plano.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(artefato), pageable, 1));

        PaginaResponseDTO<ArtefatoResponseDTO> response = service().listar(
                auth,
                plano.getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertEquals(artefato.getId(), response.conteudo().getFirst().id());
    }

    @Test
    void deveListarAgendaDoAuditorAutenticado() {
        PlanoEntity plano = plano();
        ArtefatoEntity artefato = artefato(
                documento(plano, Classificacao.AUDITADO),
                auditor(plano)
        );
        PageRequest pageable = PageRequest.of(0, 100);
        when(auth.getName()).thenReturn("auditor@quality.com");
        when(artefatoRepository
                .findAllByAuditorUsuarioEmailIgnoreCaseAndAuditorAtivoTrue(
                        "auditor@quality.com",
                        pageable
                ))
                .thenReturn(new PageImpl<>(List.of(artefato), pageable, 1));

        PaginaResponseDTO<AuditoriaAgendaResponseDTO> response =
                service().listarAgenda(auth, pageable);

        AuditoriaAgendaResponseDTO compromisso =
                response.conteudo().getFirst();
        assertEquals(1, response.totalElementos());
        assertEquals(artefato.getId(), compromisso.artefatoId());
        assertEquals(
                artefato.getAuditoria().getId(),
                compromisso.auditoriaId()
        );
        assertEquals(plano.getId(), compromisso.planoId());
        assertEquals(artefato.getDataPlanejada(), compromisso.dataPlanejada());
    }

    @Test
    void deveAtualizarDocumentoEAuditorDoArtefato() {
        PlanoEntity plano = plano();
        DocumentoEntity documentoAntigo = documento(
                plano,
                Classificacao.AUDITADO
        );
        DocumentoEntity documentoNovo = documento(
                plano,
                Classificacao.AUDITADO
        );
        ParticipacaoPlanoEntity auditorAntigo = auditor(plano);
        ParticipacaoPlanoEntity auditorNovo = auditor(plano);
        ArtefatoEntity artefato = artefato(documentoAntigo, auditorAntigo);

        autorizarGestao(plano);
        when(artefatoRepository.findByIdAndDocumentoPlanoId(
                artefato.getId(),
                plano.getId()
        )).thenReturn(Optional.of(artefato));
        when(documentoRepository.findByIdAndPlanoId(
                documentoNovo.getId(),
                plano.getId()
        )).thenReturn(Optional.of(documentoNovo));
        when(participacaoRepository.findByIdAndPlanoId(
                auditorNovo.getId(),
                plano.getId()
        )).thenReturn(Optional.of(auditorNovo));
        when(artefatoRepository
                .existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCaseAndIdNot(
                        documentoNovo.getId(),
                        "Documento de requisitos",
                        "1.0",
                        artefato.getId()
                )).thenReturn(false);
        when(artefatoRepository.save(artefato)).thenReturn(artefato);

        ArtefatoResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                artefato.getId(),
                dto(documentoNovo, auditorNovo)
        );

        assertEquals(documentoNovo.getId(), response.documentoId());
        assertEquals(auditorNovo.getId(), response.auditorParticipacaoId());
        assertEquals(documentoNovo, artefato.getDocumento());
        assertEquals(auditorNovo, artefato.getAuditor());
        assertEquals(auditorNovo, artefato.getAuditoria().getAuditor());
    }

    @Test
    void deveInformarArtefatoAusenteNoPlano() {
        PlanoEntity plano = plano();
        UUID artefatoId = UUID.randomUUID();
        when(acessoPlanoService.buscarPlano(auth, plano.getId()))
                .thenReturn(plano);
        when(artefatoRepository.findByIdAndDocumentoPlanoId(
                artefatoId,
                plano.getId()
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service().buscar(auth, plano.getId(), artefatoId)
        );
    }

    @Test
    void deveRemoverArtefatoComPermissao() {
        PlanoEntity plano = plano();
        ArtefatoEntity artefato = artefato(
                documento(plano, Classificacao.AUDITADO),
                auditor(plano)
        );
        autorizarGestao(plano);
        when(artefatoRepository.findByIdAndDocumentoPlanoId(
                artefato.getId(),
                plano.getId()
        )).thenReturn(Optional.of(artefato));

        service().remover(auth, plano.getId(), artefato.getId());

        verify(artefatoRepository).excluirComDependencias(artefato.getId());
    }

    private ArtefatoService service() {
        return new ArtefatoService(
                artefatoRepository,
                auditoriaRepository,
                documentoRepository,
                participacaoRepository,
                acessoPlanoService,
                checklistService
        );
    }

    private void autorizarGestao(PlanoEntity plano) {
        when(acessoPlanoService.buscarPlano(
                auth,
                plano.getId(),
                PermissaoPlano.GERENCIAR_ARTEFATOS
        )).thenReturn(plano);
        lenient().when(documentoRepository.findByIdAndPlanoId(
                REFERENCIA_ID,
                plano.getId()
        )).thenReturn(Optional.of(DocumentoEntity.builder()
                .id(REFERENCIA_ID)
                .plano(plano)
                .nome("Norma de referência")
                .classificacao(Classificacao.REFERENCIA)
                .build()));
    }

    private PlanoEntity plano() {
        return PlanoEntity.builder().id(UUID.randomUUID()).build();
    }

    private DocumentoEntity documento(
            PlanoEntity plano,
            Classificacao classificacao
    ) {
        return DocumentoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .nome("Requisitos")
                .classificacao(classificacao)
                .build();
    }

    private ParticipacaoPlanoEntity auditor(PlanoEntity plano) {
        return participacao(plano, PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE);
    }

    private ParticipacaoPlanoEntity participacao(
            PlanoEntity plano,
            PapelPlano papel
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome("Auditor")
                        .email("auditor@quality.com")
                        .build())
                .papeis(EnumSet.of(papel))
                .build();
    }

    private ArtefatoRequestDTO dto(
            DocumentoEntity documento,
            ParticipacaoPlanoEntity auditor
    ) {
        return new ArtefatoRequestDTO(
                documento.getId(),
                auditor.getId(),
                Set.of(REFERENCIA_ID),
                " Documento de requisitos ",
                " 1.0 ",
                LocalDate.now().plusDays(1)
        );
    }

    private ArtefatoEntity artefato(
            DocumentoEntity documento,
            ParticipacaoPlanoEntity auditor
    ) {
        ArtefatoEntity artefato = ArtefatoEntity.builder()
                .id(UUID.randomUUID())
                .documento(documento)
                .auditor(auditor)
                .nome("Documento de requisitos")
                .versao("1.0")
                .dataPlanejada(LocalDate.now().plusDays(1))
                .status(StatusArtefato.EM_PREPARACAO)
                .criadoEm(LocalDateTime.now())
                .build();
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .artefato(artefato)
                .auditor(auditor)
                .documentosReferencia(Set.of(DocumentoEntity.builder()
                        .id(REFERENCIA_ID)
                        .plano(documento.getPlano())
                        .nome("Norma de referência")
                        .classificacao(Classificacao.REFERENCIA)
                        .build()))
                .status(StatusAuditoria.EM_PREPARACAO)
                .dataInicio(LocalDateTime.now())
                .build();
        artefato.setAuditoria(auditoria);
        return artefato;
    }
}
