package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;
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
import br.com.grupo5.Quality.database.repository.RespostaAuditoriaRepository;
import br.com.grupo5.Quality.dto.request.RespostaAuditoriaRequestDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaDetalhadaResponseDTO;
import br.com.grupo5.Quality.dto.response.AuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.ItemAuditoriaResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock
    private AuditoriaRepository auditoriaRepository;
    @Mock
    private RespostaAuditoriaRepository respostaRepository;
    @Mock
    private ArtefatoRepository artefatoRepository;
    @Mock
    private ItemChecklistRepository itemRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void deveListarAuditoriasDoArtefato() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist(artefato, StatusChecklist.PUBLICADO, 1),
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        when(acessoPlanoService.buscarPlano(auth, plano.getId()))
                .thenReturn(plano);
        localizarArtefato(plano, artefato);
        PageRequest pageable = PageRequest.of(0, 15);
        when(auditoriaRepository.findAllByArtefatoId(artefato.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(auditoria), pageable, 1));

        PaginaResponseDTO<AuditoriaResponseDTO> response = service().listar(
                auth,
                plano.getId(),
                artefato.getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertEquals(auditoria.getId(), response.conteudo().getFirst().id());
    }

    @Test
    void deveRegistrarRespostaConforme() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(itemRepository.findByIdAndChecklistAuditoriaId(
                item.getId(), auditoria.getId()
        )).thenReturn(Optional.of(item));
        when(respostaRepository.findByAuditoriaIdAndItemId(
                auditoria.getId(),
                item.getId()
        )).thenReturn(Optional.empty());
        when(respostaRepository.save(any(RespostaAuditoriaEntity.class)))
                .thenAnswer(invocation -> {
                    RespostaAuditoriaEntity resposta = invocation.getArgument(0);
                    resposta.setId(UUID.randomUUID());
                    return resposta;
                });

        ItemAuditoriaResponseDTO response = service().responder(
                auth,
                plano.getId(),
                artefato.getId(),
                auditoria.getId(),
                item.getId(),
                new RespostaAuditoriaRequestDTO(
                        ResultadoItem.CONFORME,
                        " Atende ao critério. "
                )
        );

        assertEquals(ResultadoItem.CONFORME, response.resultado());
        assertEquals("Atende ao critério.", response.observacao());
        assertEquals(1, auditoria.getConformes());
        assertEquals(new BigDecimal("100.00"), auditoria.getAderenciaPercentual());
        assertNotNull(response.respondidoEm());
    }

    @Test
    void devePermitirNaoConformeSemObservacao() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(itemRepository.findByIdAndChecklistAuditoriaId(
                item.getId(), auditoria.getId()
        )).thenReturn(Optional.of(item));
        when(respostaRepository.findByAuditoriaIdAndItemId(
                auditoria.getId(),
                item.getId()
        )).thenReturn(Optional.empty());
        when(respostaRepository.save(any(RespostaAuditoriaEntity.class)))
                .thenAnswer(invocation -> {
                    RespostaAuditoriaEntity resposta = invocation.getArgument(0);
                    resposta.setId(UUID.randomUUID());
                    return resposta;
                });

        ItemAuditoriaResponseDTO response = service().responder(
                auth,
                plano.getId(),
                artefato.getId(),
                auditoria.getId(),
                item.getId(),
                new RespostaAuditoriaRequestDTO(
                        ResultadoItem.NAO_CONFORME,
                        " "
                )
        );

        assertEquals(ResultadoItem.NAO_CONFORME, response.resultado());
        assertNull(response.observacao());
        assertEquals(1, auditoria.getNaoConformes());
    }

    @Test
    void deveSubstituirRespostaSemDuplicar() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        RespostaAuditoriaEntity resposta = resposta(
                auditoria,
                item,
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(resposta);
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(itemRepository.findByIdAndChecklistAuditoriaId(
                item.getId(), auditoria.getId()
        )).thenReturn(Optional.of(item));
        when(respostaRepository.findByAuditoriaIdAndItemId(
                auditoria.getId(),
                item.getId()
        )).thenReturn(Optional.of(resposta));
        NaoConformidadeEntity rascunho = NaoConformidadeEntity.builder()
                .id(UUID.randomUUID())
                .resposta(resposta)
                .status(br.com.grupo5.Quality.database.enums.StatusNaoConformidade.RASCUNHO)
                .build();
        when(naoConformidadeRepository.findByRespostaId(resposta.getId()))
                .thenReturn(Optional.of(rascunho));
        when(respostaRepository.save(resposta)).thenReturn(resposta);

        service().responder(
                auth,
                plano.getId(),
                artefato.getId(),
                auditoria.getId(),
                item.getId(),
                new RespostaAuditoriaRequestDTO(
                        ResultadoItem.CONFORME,
                        null
                )
        );

        assertEquals(1, auditoria.getRespostas().size());
        assertEquals(1, auditoria.getConformes());
        assertEquals(0, auditoria.getNaoConformes());
        verify(naoConformidadeRepository).delete(rascunho);
        assertNull(resposta.getObservacao());
    }

    @Test
    void naoDeveAlterarResultadoQuandoRespostaPossuiNc() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        RespostaAuditoriaEntity resposta = resposta(
                auditoria,
                item,
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(resposta);
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(itemRepository.findByIdAndChecklistAuditoriaId(
                item.getId(), auditoria.getId()
        )).thenReturn(Optional.of(item));
        when(respostaRepository.findByAuditoriaIdAndItemId(
                auditoria.getId(),
                item.getId()
        )).thenReturn(Optional.of(resposta));
        NaoConformidadeEntity enviada = NaoConformidadeEntity.builder()
                .resposta(resposta)
                .status(StatusNaoConformidade.ENVIADA)
                .build();
        when(naoConformidadeRepository.findByRespostaId(resposta.getId()))
                .thenReturn(Optional.of(enviada));

        assertThrows(
                InvalidRequestException.class,
                () -> service().responder(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId(),
                        item.getId(),
                        new RespostaAuditoriaRequestDTO(
                                ResultadoItem.CONFORME,
                                null
                        )
                )
        );

        verify(respostaRepository, never()).save(any());
    }

    @Test
    void devePermitirAlterarResultadoQuandoNcFoiConcluida() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        RespostaAuditoriaEntity resposta = resposta(
                auditoria,
                item,
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(resposta);
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(itemRepository.findByIdAndChecklistAuditoriaId(
                item.getId(), auditoria.getId()
        )).thenReturn(Optional.of(item));
        when(respostaRepository.findByAuditoriaIdAndItemId(
                auditoria.getId(), item.getId()
        )).thenReturn(Optional.of(resposta));
        NaoConformidadeEntity concluida = NaoConformidadeEntity.builder()
                .resposta(resposta)
                .status(StatusNaoConformidade.CONCLUIDA)
                .build();
        when(naoConformidadeRepository.findByRespostaId(resposta.getId()))
                .thenReturn(Optional.of(concluida));
        when(respostaRepository.save(resposta)).thenReturn(resposta);

        service().responder(
                auth,
                plano.getId(),
                artefato.getId(),
                auditoria.getId(),
                item.getId(),
                new RespostaAuditoriaRequestDTO(
                        ResultadoItem.CONFORME,
                        null
                )
        );

        assertEquals(ResultadoItem.CONFORME, resposta.getResultado());
        assertEquals(1, auditoria.getConformes());
        assertEquals(0, auditoria.getNaoConformes());
        verify(naoConformidadeRepository, never()).delete(any());
    }

    @Test
    void deveMarcarItemConformeAoConcluirResolucao() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        ItemChecklistEntity item = checklist.getItens().getFirst();
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        RespostaAuditoriaEntity resposta = resposta(
                auditoria,
                item,
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(resposta);
        auditoria.setNaoConformes(1);
        NaoConformidadeEntity concluida = NaoConformidadeEntity.builder()
                .resposta(resposta)
                .status(StatusNaoConformidade.CONCLUIDA)
                .build();

        service().marcarConformeAposResolucao(concluida);

        assertEquals(ResultadoItem.CONFORME, resposta.getResultado());
        assertNull(resposta.getObservacao());
        assertEquals(1, auditoria.getConformes());
        assertEquals(0, auditoria.getNaoConformes());
        assertEquals(BigDecimal.valueOf(100).setScale(2),
                auditoria.getAderenciaPercentual());
        verify(respostaRepository).save(resposta);
        verify(auditoriaRepository).save(auditoria);
    }
    @Test
    void deveConcluirECalcularAderenciaDesconsiderandoNaoAplicavel() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                3
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        auditoria.getRespostas().add(resposta(
                auditoria,
                checklist.getItens().get(0),
                ResultadoItem.CONFORME
        ));
        RespostaAuditoriaEntity respostaNaoConforme = resposta(
                auditoria,
                checklist.getItens().get(1),
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(respostaNaoConforme);
        auditoria.getRespostas().add(resposta(
                auditoria,
                checklist.getItens().get(2),
                ResultadoItem.NAO_APLICAVEL
        ));
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(naoConformidadeRepository.findAllByRespostaAuditoriaId(
                auditoria.getId()
        )).thenReturn(List.of(NaoConformidadeEntity.builder()
                .resposta(respostaNaoConforme)
                .status(StatusNaoConformidade.CONCLUIDA)
                .build()));
        when(auditoriaRepository.save(auditoria)).thenReturn(auditoria);

        AuditoriaDetalhadaResponseDTO response = service().concluir(
                auth,
                plano.getId(),
                artefato.getId(),
                auditoria.getId()
        );

        assertEquals(StatusAuditoria.CONCLUIDA, response.status());
        assertEquals(new BigDecimal("50.00"), response.aderenciaPercentual());
        assertEquals(1, response.conformes());
        assertEquals(1, response.naoConformes());
        assertEquals(1, response.naoAplicaveis());
        assertNotNull(response.dataFim());
        assertEquals(StatusArtefato.CONCLUIDO, artefato.getStatus());
        assertEquals(StatusChecklist.CONCLUIDO, checklist.getStatus());
    }

    @Test
    void naoDeveConcluirComChecklistIndisponivel() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.RASCUNHO,
                1
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        auditoria.getRespostas().add(resposta(
                auditoria,
                checklist.getItens().getFirst(),
                ResultadoItem.CONFORME
        ));
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);

        assertThrows(
                InvalidRequestException.class,
                () -> service().concluir(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId()
                )
        );

        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void naoDeveConcluirComNaoConformidadeSomenteEmRascunho() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        RespostaAuditoriaEntity resposta = resposta(
                auditoria,
                checklist.getItens().getFirst(),
                ResultadoItem.NAO_CONFORME
        );
        auditoria.getRespostas().add(resposta);
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);
        when(naoConformidadeRepository.findAllByRespostaAuditoriaId(
                auditoria.getId()
        )).thenReturn(List.of(NaoConformidadeEntity.builder()
                .resposta(resposta)
                .status(StatusNaoConformidade.RASCUNHO)
                .build()));

        assertThrows(
                InvalidRequestException.class,
                () -> service().concluir(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId()
                )
        );

        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void naoDeveConcluirComRespostaNaoConformeSemNc() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        auditoria.getRespostas().add(resposta(
                auditoria,
                checklist.getItens().getFirst(),
                ResultadoItem.NAO_CONFORME
        ));
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);

        assertThrows(
                InvalidRequestException.class,
                () -> service().concluir(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId()
                )
        );

        verify(auditoriaRepository, never()).save(any());
    }
    @Test
    void naoDeveConcluirComItensPendentes() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                2
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.EM_ANDAMENTO
        );
        auditoria.getRespostas().add(resposta(
                auditoria,
                checklist.getItens().getFirst(),
                ResultadoItem.CONFORME
        ));
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);

        assertThrows(
                InvalidRequestException.class,
                () -> service().concluir(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId()
                )
        );
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void naoDeveResponderAuditoriaConcluida() {
        PlanoEntity plano = plano();
        ParticipacaoPlanoEntity auditor = auditor(plano);
        ArtefatoEntity artefato = artefato(plano, auditor);
        ChecklistEntity checklist = checklist(
                artefato,
                StatusChecklist.PUBLICADO,
                1
        );
        AuditoriaEntity auditoria = auditoria(
                artefato,
                checklist,
                auditor,
                StatusAuditoria.CONCLUIDA
        );
        autorizarExecucao(plano, auditor);
        localizarAuditoria(plano, artefato, auditoria);

        assertThrows(
                InvalidRequestException.class,
                () -> service().responder(
                        auth,
                        plano.getId(),
                        artefato.getId(),
                        auditoria.getId(),
                        checklist.getItens().getFirst().getId(),
                        new RespostaAuditoriaRequestDTO(
                                ResultadoItem.CONFORME,
                                null
                        )
                )
        );
        verify(itemRepository, never())
                .findByIdAndChecklistAuditoriaId(any(), any());
    }

    private AuditoriaService service() {
        return new AuditoriaService(
                auditoriaRepository,
                respostaRepository,
                artefatoRepository,
                itemRepository,
                naoConformidadeRepository,
                acessoPlanoService
        );
    }

    private void autorizarExecucao(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor
    ) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                plano.getId(),
                PermissaoPlano.AUDITAR
        )).thenReturn(auditor);
    }

    private void localizarArtefato(
            PlanoEntity plano,
            ArtefatoEntity artefato
    ) {
        when(artefatoRepository.findByIdAndDocumentoPlanoId(
                artefato.getId(),
                plano.getId()
        )).thenReturn(Optional.of(artefato));
    }

    private void localizarAuditoria(
            PlanoEntity plano,
            ArtefatoEntity artefato,
            AuditoriaEntity auditoria
    ) {
        when(auditoriaRepository
                .findByIdAndArtefatoIdAndArtefatoDocumentoPlanoId(
                        auditoria.getId(),
                        artefato.getId(),
                        plano.getId()
                )).thenReturn(Optional.of(auditoria));
    }

    private PlanoEntity plano() {
        return PlanoEntity.builder().id(UUID.randomUUID()).build();
    }

    private ParticipacaoPlanoEntity auditor(PlanoEntity plano) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome("Auditor")
                        .email("auditor@quality.com")
                        .build())
                .build();
    }

    private ArtefatoEntity artefato(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor
    ) {
        return ArtefatoEntity.builder()
                .id(UUID.randomUUID())
                .documento(DocumentoEntity.builder()
                        .id(UUID.randomUUID())
                        .plano(plano)
                        .build())
                .auditor(auditor)
                .status(StatusArtefato.EM_PREPARACAO)
                .build();
    }

    private ChecklistEntity checklist(
            ArtefatoEntity artefato,
            StatusChecklist status,
            int totalItens
    ) {
        ChecklistEntity checklist = ChecklistEntity.builder()
                .id(UUID.randomUUID())
                .versao("1.0")
                .status(status)
                .criadoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();

        for (int ordem = 1; ordem <= totalItens; ordem++) {
            checklist.getItens().add(ItemChecklistEntity.builder()
                    .id(UUID.randomUUID())
                    .checklist(checklist)
                    .ordem(ordem)
                    .pergunta("Pergunta " + ordem)
                    .origem(OrigemItemChecklist.MANUAL)
                    .criadoEm(LocalDateTime.now())
                    .build());
        }
        return checklist;
    }

    private AuditoriaEntity auditoria(
            ArtefatoEntity artefato,
            ChecklistEntity checklist,
            ParticipacaoPlanoEntity auditor,
            StatusAuditoria status
    ) {
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .artefato(artefato)
                .auditor(auditor)
                .status(status)
                .dataInicio(LocalDateTime.now())
                .checklists(new ArrayList<>(List.of(checklist)))
                .build();
        checklist.setAuditoria(auditoria);
        artefato.setAuditoria(auditoria);
        return auditoria;
    }

    private RespostaAuditoriaEntity resposta(
            AuditoriaEntity auditoria,
            ItemChecklistEntity item,
            ResultadoItem resultado
    ) {
        LocalDateTime agora = LocalDateTime.now();
        return RespostaAuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditoria(auditoria)
                .item(item)
                .resultado(resultado)
                .observacao(
                        resultado == ResultadoItem.NAO_CONFORME
                                ? "Não atende."
                                : null
                )
                .respondidoEm(agora)
                .atualizadoEm(agora)
                .build();
    }
}
