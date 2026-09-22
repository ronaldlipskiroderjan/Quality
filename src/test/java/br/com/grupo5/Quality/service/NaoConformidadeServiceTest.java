package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.ConfiguracaoClassificacaoEntity;
import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ChecklistEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.ClassificacaoNaoConformidade;
import br.com.grupo5.Quality.database.enums.OrigemItemChecklist;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.ResultadoItem;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.database.repository.RespostaAuditoriaRepository;
import br.com.grupo5.Quality.dto.request.AtualizarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.request.CriarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.response.NaoConformidadeResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaoConformidadeServiceTest {

    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private RespostaAuditoriaRepository respostaRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private ConfiguracaoPlanoService configuracaoPlanoService;
    @Mock
    private EncaminhamentoNcService encaminhamentoNcService;
    @Mock
    private AcessoNaoConformidadeService acessoNaoConformidadeService;
    @Mock
    private CalendarioPrazoService calendarioPrazoService;
    @Mock
    private Authentication auth;

    @Test
    void deveCriarEEncaminharNaoConformidadeParaEquipe() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        localizarResposta(cenario);
        autorizarAuditor(cenario);
        when(naoConformidadeRepository.existsByRespostaId(
                cenario.resposta().getId()
        )).thenReturn(false);
        localizarResponsavel(cenario);
        when(configuracaoPlanoService.buscarAtiva(
                cenario.plano().getId(),
                ClassificacaoNaoConformidade.COMPLEXA
        )).thenReturn(configuracao(cenario.plano(), 48));
        when(naoConformidadeRepository.saveAndFlush(any(NaoConformidadeEntity.class)))
                .thenAnswer(invocation -> {
                    NaoConformidadeEntity nc = invocation.getArgument(0);
                    nc.setId(UUID.randomUUID());
                    return nc;
                });
        org.mockito.Mockito.doAnswer(invocation -> {
            NaoConformidadeEntity nc = org.mockito.Mockito.mockingDetails(
                    naoConformidadeRepository
            ).getInvocations().stream()
                    .filter(call -> call.getMethod().getName().equals("saveAndFlush"))
                    .reduce((first, second) -> second)
                    .map(call -> (NaoConformidadeEntity) call.getArgument(0))
                    .orElseThrow();
            nc.setStatus(StatusNaoConformidade.ENVIADA);
            nc.setEnviadaEm(OffsetDateTime.now(ZoneOffset.UTC));
            nc.setPrazoEm(nc.getEnviadaEm().plusHours(48));
            return null;
        }).when(encaminhamentoNcService).encaminhar(
                any(), any(), any(), any()
        );

        NaoConformidadeResponseDTO response = service().criar(
                auth,
                cenario.plano().getId(),
                criarRequest(cenario)
        );

        assertEquals(StatusNaoConformidade.ENVIADA, response.status());
        assertEquals(cenario.resposta().getId(), response.respostaId());
        assertEquals(
                cenario.responsavel().getId(),
                response.responsavelParticipacaoId()
        );
        assertEquals("Corrigir o documento.", response.acaoCorretiva());
    }

    @Test
    void deveCriarRascunhoAssimQueItemForMarcadoNaoConforme() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        localizarResposta(cenario);
        autorizarAuditor(cenario);
        when(naoConformidadeRepository.findByRespostaId(
                cenario.resposta().getId()
        )).thenReturn(Optional.empty());
        when(configuracaoPlanoService.buscarAtiva(
                cenario.plano().getId(),
                ClassificacaoNaoConformidade.COMPLEXA
        )).thenReturn(configuracao(cenario.plano(), 48));
        when(naoConformidadeRepository.saveAndFlush(
                any(NaoConformidadeEntity.class)
        )).thenAnswer(invocation -> {
            NaoConformidadeEntity nc = invocation.getArgument(0);
            nc.setId(UUID.randomUUID());
            return nc;
        });
        when(calendarioPrazoService.calcularPrazo(
                any(), any(), org.mockito.ArgumentMatchers.anyInt()
        )).thenAnswer(invocation -> ((OffsetDateTime) invocation.getArgument(1))
                .plusHours(((Integer) invocation.getArgument(2)).longValue()));

        NaoConformidadeResponseDTO response = service().criarRascunho(
                auth,
                cenario.plano().getId(),
                new CriarNaoConformidadeRequestDTO(
                        cenario.resposta().getId(),
                        null,
                        ClassificacaoNaoConformidade.COMPLEXA,
                        ""
                )
        );

        assertEquals(StatusNaoConformidade.RASCUNHO, response.status());
        assertNotNull(response.identificadoEm());
        assertEquals(
                response.identificadoEm().plusHours(48),
                response.prazoEm()
        );
        verify(encaminhamentoNcService, never())
                .encaminhar(any(), any(), any(), any());
    }

    @Test
    void naoDeveCriarNcParaRespostaConforme() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        cenario.resposta().setResultado(ResultadoItem.CONFORME);
        localizarResposta(cenario);
        autorizarAuditor(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        criarRequest(cenario)
                )
        );

        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void naoDeveDuplicarNcDaMesmaResposta() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        localizarResposta(cenario);
        autorizarAuditor(cenario);
        when(naoConformidadeRepository.existsByRespostaId(
                cenario.resposta().getId()
        )).thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        criarRequest(cenario)
                )
        );

        verify(participacaoRepository, never()).findByIdAndPlanoId(any(), any());
    }

    @Test
    void deveExigirResponsavelComPapelCorreto() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        cenario.responsavel().setPapeis(EnumSet.of(PapelPlano.SUPERIOR_N1));
        localizarResposta(cenario);
        autorizarAuditor(cenario);
        localizarResponsavel(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        criarRequest(cenario)
                )
        );

        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void somenteAuditorDaExecucaoPodeCriarNc() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        ParticipacaoPlanoEntity outroAuditor = participante(
                cenario.plano(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE,
                "Outro auditor"
        );
        localizarResposta(cenario);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                br.com.grupo5.Quality.database.enums.PermissaoPlano.AUDITAR
        )).thenReturn(outroAuditor);

        assertThrows(
                AccessDeniedException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        criarRequest(cenario)
                )
        );

        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void deveAtualizarRascunhoAposConclusaoDaAuditoria() {
        Cenario cenario = cenario(StatusAuditoria.CONCLUIDA);
        NaoConformidadeEntity nc = naoConformidade(cenario);
        localizarNaoConformidade(cenario, nc);
        autorizarAuditor(cenario);
        localizarResponsavel(cenario);
        when(configuracaoPlanoService.buscarAtiva(
                cenario.plano().getId(),
                ClassificacaoNaoConformidade.SEVERA
        )).thenReturn(configuracao(cenario.plano(), 72));
        when(naoConformidadeRepository.save(nc)).thenReturn(nc);

        NaoConformidadeResponseDTO response = service().atualizar(
                auth,
                cenario.plano().getId(),
                nc.getId(),
                atualizarRequest(cenario.responsavel().getId())
        );

        assertEquals(ClassificacaoNaoConformidade.SEVERA, response.classificacao());
        assertEquals("Ação revisada.", response.acaoCorretiva());
    }

    @Test
    void naoDeveAlterarNcQueNaoEstejaEmRascunho() {
        Cenario cenario = cenario(StatusAuditoria.CONCLUIDA);
        NaoConformidadeEntity nc = naoConformidade(cenario);
        nc.setStatus(StatusNaoConformidade.ENVIADA);
        localizarNaoConformidade(cenario, nc);
        autorizarAuditor(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().atualizar(
                        auth,
                        cenario.plano().getId(),
                        nc.getId(),
                        atualizarRequest(cenario.responsavel().getId())
                )
        );

        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void naoDeveExcluirRascunhoAposConclusaoDaAuditoria() {
        Cenario cenario = cenario(StatusAuditoria.CONCLUIDA);
        NaoConformidadeEntity nc = naoConformidade(cenario);
        localizarNaoConformidade(cenario, nc);
        autorizarAuditor(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().remover(
                        auth,
                        cenario.plano().getId(),
                        nc.getId()
                )
        );

        verify(naoConformidadeRepository, never()).delete(any());
    }

    @Test
    void deveListarNaoConformidadesDoPlano() {
        Cenario cenario = cenario(StatusAuditoria.EM_ANDAMENTO);
        NaoConformidadeEntity nc = naoConformidade(cenario);
        when(acessoPlanoService.buscarPlano(auth, cenario.plano().getId()))
                .thenReturn(cenario.plano());
        PageRequest pageable = PageRequest.of(0, 15);
        when(naoConformidadeRepository
                .findAllByRespostaAuditoriaArtefatoDocumentoPlanoId(
                        cenario.plano().getId(),
                        pageable
                )).thenReturn(new PageImpl<>(List.of(nc), pageable, 1));

        PaginaResponseDTO<NaoConformidadeResponseDTO> response = service().listar(
                auth,
                cenario.plano().getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
    }

    private NaoConformidadeService service() {
        return new NaoConformidadeService(
                naoConformidadeRepository,
                respostaRepository,
                participacaoRepository,
                acessoPlanoService,
                configuracaoPlanoService,
                encaminhamentoNcService,
                acessoNaoConformidadeService,
                calendarioPrazoService
        );
    }

    private void localizarResposta(Cenario cenario) {
        when(respostaRepository
                .findByIdAndAuditoriaArtefatoDocumentoPlanoId(
                        cenario.resposta().getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(cenario.resposta()));
    }

    private void localizarNaoConformidade(
            Cenario cenario,
            NaoConformidadeEntity nc
    ) {
        when(naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        nc.getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(nc));
    }

    private void autorizarAuditor(Cenario cenario) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                br.com.grupo5.Quality.database.enums.PermissaoPlano.AUDITAR
        )).thenReturn(cenario.auditor());
    }

    private void localizarResponsavel(Cenario cenario) {
        when(participacaoRepository.findByIdAndPlanoId(
                cenario.responsavel().getId(),
                cenario.plano().getId()
        )).thenReturn(Optional.of(cenario.responsavel()));
    }

    private CriarNaoConformidadeRequestDTO criarRequest(Cenario cenario) {
        return new CriarNaoConformidadeRequestDTO(
                cenario.resposta().getId(),
                cenario.responsavel().getId(),
                ClassificacaoNaoConformidade.COMPLEXA,
                " Corrigir o documento. "
        );
    }

    private AtualizarNaoConformidadeRequestDTO atualizarRequest(
            UUID responsavelId
    ) {
        return new AtualizarNaoConformidadeRequestDTO(
                responsavelId,
                ClassificacaoNaoConformidade.SEVERA,
                " Ação revisada. "
        );
    }

    private NaoConformidadeEntity naoConformidade(Cenario cenario) {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        return NaoConformidadeEntity.builder()
                .id(UUID.randomUUID())
                .resposta(cenario.resposta())
                .responsavel(cenario.responsavel())
                .classificacao(ClassificacaoNaoConformidade.COMPLEXA)
                .descricao("Falha encontrada.")
                .acaoCorretiva("Corrigir o documento.")
                .identificadoEm(agora)
                .prazoResolucaoHoras(48)
                .atualizadoEm(agora)
                .status(StatusNaoConformidade.RASCUNHO)
                .build();
    }

    private ConfiguracaoClassificacaoEntity configuracao(
            PlanoEntity plano,
            int prazoHoras
    ) {
        return ConfiguracaoClassificacaoEntity.builder()
                .plano(plano)
                .classificacao(ClassificacaoNaoConformidade.COMPLEXA)
                .prazoHoras(prazoHoras)
                .ativa(true)
                .build();
    }

    private Cenario cenario(StatusAuditoria status) {
        PlanoEntity plano = PlanoEntity.builder()
                .id(UUID.randomUUID())
                .build();
        ParticipacaoPlanoEntity auditor = participante(
                plano,
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE,
                "Auditor"
        );
        ParticipacaoPlanoEntity responsavel = participante(
                plano,
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO,
                "Responsável"
        );
        DocumentoEntity documento = DocumentoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .build();
        ArtefatoEntity artefato = ArtefatoEntity.builder()
                .id(UUID.randomUUID())
                .documento(documento)
                .auditor(auditor)
                .nome("Plano de testes")
                .build();
        ChecklistEntity checklist = ChecklistEntity.builder()
                .id(UUID.randomUUID())
                .build();
        ItemChecklistEntity item = ItemChecklistEntity.builder()
                .id(UUID.randomUUID())
                .checklist(checklist)
                .ordem(1)
                .pergunta("O documento está completo?")
                .origem(OrigemItemChecklist.MANUAL)
                .build();
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .artefato(artefato)
                .checklists(List.of(checklist))
                .auditor(auditor)
                .dataInicio(LocalDateTime.now())
                .status(status)
                .build();
        checklist.setAuditoria(auditoria);
        artefato.setAuditoria(auditoria);
        RespostaAuditoriaEntity resposta = RespostaAuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditoria(auditoria)
                .item(item)
                .resultado(ResultadoItem.NAO_CONFORME)
                .observacao("Não atende.")
                .respondidoEm(LocalDateTime.now())
                .atualizadoEm(LocalDateTime.now())
                .build();
        return new Cenario(
                plano,
                auditor,
                responsavel,
                resposta
        );
    }

    private ParticipacaoPlanoEntity participante(
            PlanoEntity plano,
            PapelPlano papel,
            String nome
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome(nome)
                        .email(nome.toLowerCase().replace("á", "a")
                                + "@quality.com")
                        .build())
                .papeis(EnumSet.of(papel))
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private record Cenario(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor,
            ParticipacaoPlanoEntity responsavel,
            RespostaAuditoriaEntity resposta
    ) {
    }
}
