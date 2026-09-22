package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.DecisaoValidacao;
import br.com.grupo5.Quality.database.enums.PapelPlano;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolucaoServiceTest {

    @Mock
    private ResolucaoNcRepository resolucaoRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private AcessoNaoConformidadeService acessoNaoConformidadeService;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private NotificacaoResolucaoService notificacaoResolucaoService;
    @Mock
    private Authentication auth;

    @Test
    void deveInformarResolucaoEEnviarParaValidacao() {
        Cenario cenario = cenario(StatusNaoConformidade.ENVIADA);
        localizarComBloqueio(cenario);
        autorizarResponsavel(cenario);
        prepararCriacaoResolucao();

        ResolucaoResponseDTO response = service().informar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                informarRequest()
        );

        assertTrue(cenario.responsavel().possuiPermissao(
                PermissaoPlano.TRATAR_NAO_CONFORMIDADE
        ));
        assertEquals(StatusResolucao.INFORMADA, response.status());
        assertEquals("Correção aplicada.", response.descricao());
        assertEquals("Evidência 123.", response.evidencia());
        assertEquals(
                StatusNaoConformidade.RESOLUCAO_INFORMADA,
                cenario.naoConformidade().getStatus()
        );
        assertNotNull(response.informadaEm());
        verify(naoConformidadeRepository)
                .save(cenario.naoConformidade());
        verify(notificacaoResolucaoService).notificarResolucaoInformada(
                eq(cenario.plano().getId()),
                eq(cenario.naoConformidade()),
                any(ResolucaoNcEntity.class),
                eq(response.informadaEm())
        );
    }

    @Test
    void devePermitirResolucaoQuandoPrazoN1EstiverVencido() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA_N1);
        localizarComBloqueio(cenario);
        autorizarResponsavel(cenario);
        prepararCriacaoResolucao();

        service().informar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                informarRequest()
        );

        assertEquals(
                StatusNaoConformidade.RESOLUCAO_INFORMADA,
                cenario.naoConformidade().getStatus()
        );
    }

    @Test
    void devePermitirResolucaoQuandoPrazoN2EstiverVencido() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA_N2);
        localizarComBloqueio(cenario);
        autorizarResponsavel(cenario);
        prepararCriacaoResolucao();

        service().informar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                informarRequest()
        );

        assertEquals(
                StatusNaoConformidade.RESOLUCAO_INFORMADA,
                cenario.naoConformidade().getStatus()
        );
    }

    @Test
    void somenteResponsavelAtribuidoPodeInformarResolucao() {
        Cenario cenario = cenario(StatusNaoConformidade.ENVIADA);
        localizarComBloqueio(cenario);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.TRATAR_NAO_CONFORMIDADE
        )).thenReturn(cenario.outroParticipante());

        assertThrows(
                AccessDeniedException.class,
                () -> service().informar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        informarRequest()
                )
        );

        verify(resolucaoRepository, never()).save(any());
    }

    @Test
    void naoDeveInformarResolucaoAntesDoEnvioDaNc() {
        Cenario cenario = cenario(StatusNaoConformidade.RASCUNHO);
        localizarComBloqueio(cenario);
        autorizarResponsavel(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().informar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        informarRequest()
                )
        );

        verify(resolucaoRepository, never()).save(any());
    }

    @Test
    void devePermitirNovaResolucaoAposPedidoDeAjustes() {
        Cenario cenario = cenario(StatusNaoConformidade.EM_TRATAMENTO);
        localizarComBloqueio(cenario);
        autorizarResponsavel(cenario);
        prepararCriacaoResolucao();

        ResolucaoResponseDTO response = service().informar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                informarRequest()
        );

        assertEquals(StatusResolucao.INFORMADA, response.status());
        assertEquals(
                StatusNaoConformidade.RESOLUCAO_INFORMADA,
                cenario.naoConformidade().getStatus()
        );
    }

    @Test
    void deveAprovarResolucaoEConcluirNc() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        localizarComBloqueio(cenario);
        autorizarAuditor(cenario);
        localizarResolucao(cenario, resolucao);

        ResolucaoResponseDTO response = service().validar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                resolucao.getId(),
                new ValidarResolucaoRequestDTO(
                        DecisaoValidacao.APROVAR,
                        " Correção conferida. "
                )
        );

        assertEquals(StatusResolucao.APROVADA, response.status());
        assertEquals("Correção conferida.", response.observacaoAuditor());
        assertEquals(
                StatusNaoConformidade.CONCLUIDA,
                cenario.naoConformidade().getStatus()
        );
        assertNotNull(cenario.naoConformidade().getConcluidaEm());
        assertEquals(
                response.validadaEm(),
                cenario.naoConformidade().getConcluidaEm()
        );
        verify(auditoriaService)
                .marcarConformeAposResolucao(cenario.naoConformidade());
        verify(notificacaoResolucaoService, never())
                .notificarAjustesSolicitados(any(), any(), any(), any());
    }

    @Test
    void deveSolicitarAjustesEReabrirTratamento() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        localizarComBloqueio(cenario);
        autorizarAuditor(cenario);
        localizarResolucao(cenario, resolucao);

        ResolucaoResponseDTO response = service().validar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                resolucao.getId(),
                new ValidarResolucaoRequestDTO(
                        DecisaoValidacao.SOLICITAR_AJUSTES,
                        " Anexar a evidência da correção. "
                )
        );

        assertEquals(
                StatusResolucao.AJUSTES_SOLICITADOS,
                response.status()
        );
        assertEquals(
                "Anexar a evidência da correção.",
                response.observacaoAuditor()
        );
        assertEquals(
                StatusNaoConformidade.EM_TRATAMENTO,
                cenario.naoConformidade().getStatus()
        );
        assertNull(cenario.naoConformidade().getConcluidaEm());
        verify(auditoriaService, never())
                .marcarConformeAposResolucao(any());
        verify(notificacaoResolucaoService).notificarAjustesSolicitados(
                eq(cenario.plano().getId()),
                eq(cenario.naoConformidade()),
                eq(resolucao),
                eq(response.validadaEm())
        );
    }

    @Test
    void deveExigirObservacaoAoSolicitarAjustes() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        localizarComBloqueio(cenario);
        autorizarAuditor(cenario);
        localizarResolucao(cenario, resolucao);

        assertThrows(
                InvalidRequestException.class,
                () -> service().validar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        resolucao.getId(),
                        new ValidarResolucaoRequestDTO(
                                DecisaoValidacao.SOLICITAR_AJUSTES,
                                " "
                        )
                )
        );

        verify(resolucaoRepository, never()).save(any());
        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void naoDeveValidarResolucaoJaValidada() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        resolucao.setStatus(StatusResolucao.APROVADA);
        localizarComBloqueio(cenario);
        autorizarAuditor(cenario);
        localizarResolucao(cenario, resolucao);

        assertThrows(
                InvalidRequestException.class,
                () -> service().validar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        resolucao.getId(),
                        new ValidarResolucaoRequestDTO(
                                DecisaoValidacao.APROVAR,
                                null
                        )
                )
        );

        verify(resolucaoRepository, never()).save(any());
    }

    @Test
    void somenteAuditorDaExecucaoPodeValidarResolucao() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA
        );
        localizarComBloqueio(cenario);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.AUDITAR
        )).thenReturn(cenario.outroParticipante());

        assertThrows(
                AccessDeniedException.class,
                () -> service().validar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        UUID.randomUUID(),
                        new ValidarResolucaoRequestDTO(
                                DecisaoValidacao.APROVAR,
                                null
                        )
                )
        );

        verify(resolucaoRepository, never())
                .findByIdAndNaoConformidadeId(any(), any());
    }

    @Test
    void deveListarHistoricoDeResolucoes() {
        Cenario cenario = cenario(StatusNaoConformidade.EM_TRATAMENTO);
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        localizarNaoConformidade(cenario);
        PageRequest pageable = PageRequest.of(0, 15);
        when(resolucaoRepository.findAllByNaoConformidadeId(
                cenario.naoConformidade().getId(),
                pageable
        )).thenReturn(new PageImpl<>(List.of(resolucao), pageable, 1));

        PaginaResponseDTO<ResolucaoResponseDTO> response = service().listar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertEquals(resolucao.getId(), response.conteudo().getFirst().id());
    }

    @Test
    void deveBuscarResolucaoDoPlano() {
        Cenario cenario = cenario(StatusNaoConformidade.EM_TRATAMENTO);
        ResolucaoNcEntity resolucao = resolucaoInformada(cenario);
        localizarNaoConformidade(cenario);
        localizarResolucao(cenario, resolucao);

        ResolucaoResponseDTO response = service().buscar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                resolucao.getId()
        );

        assertEquals(resolucao.getId(), response.id());
        assertEquals(
                cenario.responsavel().getId(),
                response.responsavelParticipacaoId()
        );
    }

    private ResolucaoService service() {
        return new ResolucaoService(
                resolucaoRepository,
                naoConformidadeRepository,
                acessoPlanoService,
                acessoNaoConformidadeService,
                auditoriaService,
                notificacaoResolucaoService
        );
    }

    private void localizarComBloqueio(Cenario cenario) {
        when(naoConformidadeRepository.buscarParaAtualizacao(
                cenario.naoConformidade().getId(),
                cenario.plano().getId()
        )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private void localizarNaoConformidade(Cenario cenario) {
        when(naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        cenario.naoConformidade().getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private void autorizarResponsavel(Cenario cenario) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.TRATAR_NAO_CONFORMIDADE
        )).thenReturn(cenario.responsavel());
    }

    private void autorizarAuditor(Cenario cenario) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.AUDITAR
        )).thenReturn(cenario.auditor());
    }

    private void prepararCriacaoResolucao() {
        when(resolucaoRepository.save(any(ResolucaoNcEntity.class)))
                .thenAnswer(invocation -> {
                    ResolucaoNcEntity resolucao = invocation.getArgument(0);
                    resolucao.setId(UUID.randomUUID());
                    return resolucao;
                });
    }

    private void localizarResolucao(
            Cenario cenario,
            ResolucaoNcEntity resolucao
    ) {
        when(resolucaoRepository.findByIdAndNaoConformidadeId(
                resolucao.getId(),
                cenario.naoConformidade().getId()
        )).thenReturn(Optional.of(resolucao));
    }

    private InformarResolucaoRequestDTO informarRequest() {
        return new InformarResolucaoRequestDTO(
                " Correção aplicada. ",
                " Evidência 123. "
        );
    }

    private ResolucaoNcEntity resolucaoInformada(Cenario cenario) {
        return ResolucaoNcEntity.builder()
                .id(UUID.randomUUID())
                .naoConformidade(cenario.naoConformidade())
                .responsavel(cenario.responsavel())
                .descricao("Correção aplicada.")
                .evidencia("Evidência 123.")
                .status(StatusResolucao.INFORMADA)
                .informadaEm(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private Cenario cenario(StatusNaoConformidade status) {
        PlanoEntity plano = PlanoEntity.builder()
                .id(UUID.randomUUID())
                .build();
        ParticipacaoPlanoEntity auditor = participante(
                plano,
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE,
                "Auditor",
                "auditor@quality.com"
        );
        ParticipacaoPlanoEntity responsavel = participante(
                plano,
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO,
                "Responsável",
                "responsavel@quality.com"
        );
        ParticipacaoPlanoEntity outroParticipante = participante(
                plano,
                PapelPlano.SUPERIOR_N1,
                "Participante",
                "participante@quality.com"
        );
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .build();
        RespostaAuditoriaEntity resposta = RespostaAuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditoria(auditoria)
                .build();
        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .id(UUID.randomUUID())
                        .resposta(resposta)
                        .responsavel(responsavel)
                        .status(status)
                        .atualizadoEm(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();

        return new Cenario(
                plano,
                auditor,
                responsavel,
                outroParticipante,
                naoConformidade
        );
    }

    private ParticipacaoPlanoEntity participante(
            PlanoEntity plano,
            PapelPlano papel,
            String nome,
            String email
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome(nome)
                        .email(email)
                        .build())
                .papeis(EnumSet.of(papel))
                .build();
    }

    private record Cenario(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor,
            ParticipacaoPlanoEntity responsavel,
            ParticipacaoPlanoEntity outroParticipante,
            NaoConformidadeEntity naoConformidade
    ) {
    }
}
