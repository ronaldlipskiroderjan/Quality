package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.ItemChecklistEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.request.CriarEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.request.RevisarPrazoEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoPainelResponseDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscalonamentoServiceTest {

    private static final String CHAVE = "escalonamento-n1-001";

    @Mock
    private EscalonamentoNcRepository escalonamentoRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private CalendarioPrazoService calendarioPrazoService;
    @Mock
    private Authentication auth;

    @Test
    void deveEscalonarNcVencidaParaN1() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA);
        prepararCriacao(cenario);
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(cenario.responsavelN1()));
        when(escalonamentoRepository.saveAndFlush(
                any(EscalonamentoNcEntity.class)
        )).thenAnswer(invocation -> {
            EscalonamentoNcEntity escalonamento = invocation.getArgument(0);
            escalonamento.setId(UUID.randomUUID());
            return escalonamento;
        });
        when(calendarioPrazoService.calcularPrazo(
                any(), any(), org.mockito.ArgumentMatchers.anyInt()
        )).thenAnswer(invocation -> ((OffsetDateTime) invocation.getArgument(1))
                .plusHours(((Integer) invocation.getArgument(2)).longValue()));

        CriacaoEscalonamentoResultado resultado = service().criar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                CHAVE,
                request()
        );

        assertTrue(resultado.criado());
        assertEquals(NivelEscalonamento.N1, resultado.escalonamento().nivel());
        assertEquals(
                cenario.responsavelN1().getId(),
                resultado.escalonamento().responsavelParticipacaoId()
        );
        assertEquals("Analisar impacto.", resultado.escalonamento().observacao());
        assertEquals(
                resultado.escalonamento().escalonadoEm().plusHours(24),
                resultado.escalonamento().prazoEm()
        );
        assertEquals(
                StatusNaoConformidade.ESCALONADA_N1,
                cenario.naoConformidade().getStatus()
        );
        assertEquals(
                resultado.escalonamento().prazoEm(),
                cenario.naoConformidade().getPrazoEm()
        );

        ArgumentCaptor<NotificacaoEntity> captor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository).save(captor.capture());
        assertEquals(
                TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N1,
                captor.getValue().getTipo()
        );
        assertEquals(
                StatusNotificacao.NAO_LIDA,
                captor.getValue().getStatus()
        );
        assertEquals(
                cenario.responsavelN1(),
                captor.getValue().getDestinatario()
        );
        verify(naoConformidadeRepository)
                .save(cenario.naoConformidade());
    }

    @Test
    void repeticaoComMesmaChaveDeveRetornarEscalonamentoExistente() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        EscalonamentoNcEntity existente = escalonamento(cenario);
        localizarEAutorizar(cenario);
        when(escalonamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        CHAVE
                )).thenReturn(Optional.of(existente));

        CriacaoEscalonamentoResultado resultado = service().criar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                CHAVE,
                request()
        );

        assertFalse(resultado.criado());
        assertEquals(existente.getId(), resultado.escalonamento().id());
        verify(notificacaoRepository, never()).save(any());
        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void naoDeveReutilizarChaveComOutroConteudo() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        EscalonamentoNcEntity existente = escalonamento(cenario);
        localizarEAutorizar(cenario);
        when(escalonamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        CHAVE
                )).thenReturn(Optional.of(existente));

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        new CriarEscalonamentoRequestDTO(
                                NivelEscalonamento.N1,
                                48,
                                "Outro conteúdo"
                        )
                )
        );

        verify(escalonamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void somenteAuditorDaExecucaoPodeEscalonar() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA);
        ParticipacaoPlanoEntity outroAuditor = participante(
                cenario.plano(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE,
                "Outro auditor",
                "outro-auditor@quality.com"
        );
        localizar(cenario);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.ESCALONAR_NAO_CONFORMIDADE
        )).thenReturn(outroAuditor);

        assertThrows(
                AccessDeniedException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        request()
                )
        );

        verify(escalonamentoRepository, never())
                .findByNaoConformidadeIdAndChaveIdempotencia(any(), any());
    }

    @Test
    void naoDeveEscalonarNcQueNaoEstejaVencida() {
        Cenario cenario = cenario(StatusNaoConformidade.ENVIADA);
        prepararCriacao(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        request()
                )
        );

        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
    }

    @Test
    void deveExigirResponsavelN1NoPlano() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA);
        prepararCriacao(cenario);
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of());

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        request()
                )
        );

        verify(escalonamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRecusarPlanoComMaisDeUmResponsavelN1() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA);
        prepararCriacao(cenario);
        ParticipacaoPlanoEntity outroResponsavel = participante(
                cenario.plano(),
                PapelPlano.SUPERIOR_N1,
                "Outro responsável",
                "outro-n1@quality.com"
        );
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(
                cenario.responsavelN1(),
                outroResponsavel
        ));

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        request()
                )
        );
    }

    @Test
    void deveEscalonarNcVencidaNoN1ParaN2() {
        Cenario cenario = cenario(StatusNaoConformidade.VENCIDA_N1);
        prepararCriacao(cenario);
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N2
        )).thenReturn(List.of(cenario.responsavelN2()));
        when(escalonamentoRepository.saveAndFlush(
                any(EscalonamentoNcEntity.class)
        )).thenAnswer(invocation -> {
            EscalonamentoNcEntity escalonamento = invocation.getArgument(0);
            escalonamento.setId(UUID.randomUUID());
            return escalonamento;
        });

        CriacaoEscalonamentoResultado resultado = service().criar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                CHAVE,
                requestN2()
        );

        assertTrue(resultado.criado());
        assertEquals(NivelEscalonamento.N2, resultado.escalonamento().nivel());
        assertEquals(
                cenario.responsavelN2().getId(),
                resultado.escalonamento().responsavelParticipacaoId()
        );
        assertEquals(
                StatusNaoConformidade.ESCALONADA_N2,
                cenario.naoConformidade().getStatus()
        );

        ArgumentCaptor<NotificacaoEntity> captor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository).save(captor.capture());
        assertEquals(
                TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N2,
                captor.getValue().getTipo()
        );
        assertEquals(
                cenario.responsavelN2(),
                captor.getValue().getDestinatario()
        );
    }

    @Test
    void naoDeveEscalonarParaN2AntesDoVencimentoN1() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        prepararCriacao(cenario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().criar(
                        auth,
                        cenario.plano().getId(),
                        cenario.naoConformidade().getId(),
                        CHAVE,
                        requestN2()
                )
        );

        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
    }

    @Test
    void deveListarHistoricoDeEscalonamentos() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        EscalonamentoNcEntity escalonamento = escalonamento(cenario);
        when(acessoPlanoService.buscarPlano(
                auth,
                cenario.plano().getId()
        )).thenReturn(cenario.plano());
        when(naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        cenario.naoConformidade().getId(),
                        cenario.plano().getId()
                )).thenReturn(Optional.of(cenario.naoConformidade()));
        PageRequest pageable = PageRequest.of(0, 15);
        when(escalonamentoRepository.listarPorNaoConformidade(
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                pageable
        )).thenReturn(new PageImpl<>(List.of(escalonamento), pageable, 1));

        PaginaResponseDTO<EscalonamentoResponseDTO> response = service().listar(
                auth,
                cenario.plano().getId(),
                cenario.naoConformidade().getId(),
                pageable
        );

        assertEquals(1, response.totalElementos());
        assertEquals(escalonamento.getId(), response.conteudo().getFirst().id());
    }

    @Test
    void deveOcultarEscalonamentoForaDoPlano() {
        UUID planoId = UUID.randomUUID();
        UUID naoConformidadeId = UUID.randomUUID();
        UUID escalonamentoId = UUID.randomUUID();
        when(acessoPlanoService.buscarPlano(auth, planoId))
                .thenReturn(PlanoEntity.builder().id(planoId).build());
        when(escalonamentoRepository.buscarPorId(
                planoId,
                naoConformidadeId,
                escalonamentoId
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service().buscar(
                        auth,
                        planoId,
                        naoConformidadeId,
                        escalonamentoId
                )
        );
    }

    @Test
    void superiorResponsavelDeveDefinirNovoPrazo() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        EscalonamentoNcEntity escalonamento = escalonamento(cenario);
        OffsetDateTime novoPrazo = OffsetDateTime.now(ZoneOffset.UTC)
                .plusDays(2);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.GERENCIAR_ESCALONAMENTOS
        )).thenReturn(cenario.responsavelN1());
        when(escalonamentoRepository.buscarParaRevisao(
                cenario.plano().getId(),
                cenario.responsavelN1().getId(),
                escalonamento.getId()
        )).thenReturn(Optional.of(escalonamento));
        when(escalonamentoRepository.save(escalonamento))
                .thenReturn(escalonamento);

        EscalonamentoPainelResponseDTO response = service().revisarPrazo(
                auth,
                cenario.plano().getId(),
                escalonamento.getId(),
                new RevisarPrazoEscalonamentoRequestDTO(novoPrazo)
        );

        assertEquals(novoPrazo, response.prazoEm());
        assertEquals(novoPrazo, cenario.naoConformidade().getPrazoEm());
        assertEquals("Artefato auditado", response.artefatoNome());
        verify(naoConformidadeRepository).save(cenario.naoConformidade());
    }

    @Test
    void devePermitirQueSuperiorMantenhaPrazoAtual() {
        Cenario cenario = cenario(StatusNaoConformidade.ESCALONADA_N1);
        EscalonamentoNcEntity escalonamento = escalonamento(cenario);
        escalonamento.setPrazoEm(
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(5)
        );
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.GERENCIAR_ESCALONAMENTOS
        )).thenReturn(cenario.responsavelN1());
        when(escalonamentoRepository.buscarParaRevisao(
                cenario.plano().getId(),
                cenario.responsavelN1().getId(),
                escalonamento.getId()
        )).thenReturn(Optional.of(escalonamento));
        when(escalonamentoRepository.save(escalonamento))
                .thenReturn(escalonamento);

        EscalonamentoPainelResponseDTO response = service().revisarPrazo(
                auth,
                cenario.plano().getId(),
                escalonamento.getId(),
                new RevisarPrazoEscalonamentoRequestDTO(
                        escalonamento.getPrazoEm()
                )
        );

        assertEquals(escalonamento.getPrazoEm(), response.prazoEm());
    }

    private EscalonamentoService service() {
        return new EscalonamentoService(
                escalonamentoRepository,
                naoConformidadeRepository,
                participacaoRepository,
                notificacaoRepository,
                acessoPlanoService,
                calendarioPrazoService
        );
    }

    private void prepararCriacao(Cenario cenario) {
        localizarEAutorizar(cenario);
        when(escalonamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        CHAVE
                )).thenReturn(Optional.empty());
    }

    private void localizarEAutorizar(Cenario cenario) {
        localizar(cenario);
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.plano().getId(),
                PermissaoPlano.ESCALONAR_NAO_CONFORMIDADE
        )).thenReturn(cenario.auditor());
    }

    private void localizar(Cenario cenario) {
        when(naoConformidadeRepository.buscarParaAtualizacao(
                cenario.naoConformidade().getId(),
                cenario.plano().getId()
        )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private CriarEscalonamentoRequestDTO request() {
        return new CriarEscalonamentoRequestDTO(
                NivelEscalonamento.N1,
                24,
                " Analisar impacto. "
        );
    }

    private CriarEscalonamentoRequestDTO requestN2() {
        return new CriarEscalonamentoRequestDTO(
                NivelEscalonamento.N2,
                48,
                "Escalar para a gestão."
        );
    }

    private EscalonamentoNcEntity escalonamento(Cenario cenario) {
        OffsetDateTime escalonadoEm =
                OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        return EscalonamentoNcEntity.builder()
                .id(UUID.randomUUID())
                .naoConformidade(cenario.naoConformidade())
                .chaveIdempotencia(CHAVE)
                .nivel(NivelEscalonamento.N1)
                .responsavel(cenario.responsavelN1())
                .auditor(cenario.auditor())
                .observacao("Analisar impacto.")
                .prazoHoras(24)
                .escalonadoEm(escalonadoEm)
                .prazoOriginalEm(escalonadoEm.plusHours(24))
                .prazoEm(escalonadoEm.plusHours(24))
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
        ParticipacaoPlanoEntity responsavelN1 = participante(
                plano,
                PapelPlano.SUPERIOR_N1,
                "Responsável N1",
                "responsavel-n1@quality.com"
        );
        ParticipacaoPlanoEntity responsavelN2 = participante(
                plano,
                PapelPlano.SUPERIOR_N2,
                "Responsável N2",
                "responsavel-n2@quality.com"
        );
        DocumentoEntity documento = DocumentoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .build();
        ArtefatoEntity artefato = ArtefatoEntity.builder()
                .id(UUID.randomUUID())
                .nome("Artefato auditado")
                .documento(documento)
                .build();
        AuditoriaEntity auditoria = AuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditor(auditor)
                .artefato(artefato)
                .build();
        ItemChecklistEntity item = ItemChecklistEntity.builder()
                .id(UUID.randomUUID())
                .ordem(3)
                .pergunta("A evidência está completa?")
                .build();
        RespostaAuditoriaEntity resposta = RespostaAuditoriaEntity.builder()
                .id(UUID.randomUUID())
                .auditoria(auditoria)
                .item(item)
                .build();
        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .id(UUID.randomUUID())
                        .resposta(resposta)
                        .status(status)
                        .atualizadoEm(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();

        return new Cenario(
                plano,
                auditor,
                responsavelN1,
                responsavelN2,
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
            ParticipacaoPlanoEntity responsavelN1,
            ParticipacaoPlanoEntity responsavelN2,
            NaoConformidadeEntity naoConformidade
    ) {
    }
}
