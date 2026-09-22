package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VencimentoNaoConformidadeServiceTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(
            2026, 9, 13, 18, 0, 0, 0, ZoneOffset.UTC
    );
    private static final OffsetDateTime NOVO_PRAZO = AGORA.plusHours(24);

    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private EscalonamentoNcRepository escalonamentoRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private CalendarioPrazoService calendarioPrazoService;

    @Test
    void deveEscalonarNcVencidaImediatamenteParaN1() {
        Cenario cenario = cenario(
                StatusNaoConformidade.EM_TRATAMENTO,
                AGORA.minusMinutes(1)
        );
        ParticipacaoPlanoEntity superior = superior(cenario.plano(), PapelPlano.SUPERIOR_N1);
        localizar(cenario);
        prepararEscalonamento(cenario, PapelPlano.SUPERIOR_N1, superior);

        boolean processada = service().processar(
                cenario.naoConformidade().getId(),
                AGORA
        );

        assertTrue(processada);
        assertEquals(
                StatusNaoConformidade.ESCALONADA_N1,
                cenario.naoConformidade().getStatus()
        );
        assertEquals(NOVO_PRAZO, cenario.naoConformidade().getPrazoEm());
        assertEquals(AGORA, cenario.naoConformidade().getAtualizadoEm());

        ArgumentCaptor<EscalonamentoNcEntity> escalonamentoCaptor =
                ArgumentCaptor.forClass(EscalonamentoNcEntity.class);
        verify(escalonamentoRepository).saveAndFlush(
                escalonamentoCaptor.capture()
        );
        EscalonamentoNcEntity escalonamento = escalonamentoCaptor.getValue();
        assertEquals(NivelEscalonamento.N1, escalonamento.getNivel());
        assertEquals(superior, escalonamento.getResponsavel());
        assertEquals(24, escalonamento.getPrazoHoras());
        assertEquals(NOVO_PRAZO, escalonamento.getPrazoEm());

        ArgumentCaptor<NotificacaoEntity> notificacaoCaptor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository).save(notificacaoCaptor.capture());
        NotificacaoEntity notificacao = notificacaoCaptor.getValue();
        assertEquals(superior, notificacao.getDestinatario());
        assertEquals(
                TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N1,
                notificacao.getTipo()
        );
        assertEquals(StatusNotificacao.NAO_LIDA, notificacao.getStatus());
        verify(naoConformidadeRepository)
                .save(cenario.naoConformidade());
    }

    @Test
    void deveEscalonarPrazoN1VencidoImediatamenteParaN2() {
        Cenario cenario = cenario(
                StatusNaoConformidade.ESCALONADA_N1,
                AGORA.minusMinutes(1)
        );
        ParticipacaoPlanoEntity superior = superior(cenario.plano(), PapelPlano.SUPERIOR_N2);
        localizar(cenario);
        prepararEscalonamento(cenario, PapelPlano.SUPERIOR_N2, superior);

        boolean processada = service().processar(
                cenario.naoConformidade().getId(),
                AGORA
        );

        assertTrue(processada);
        assertEquals(
                StatusNaoConformidade.ESCALONADA_N2,
                cenario.naoConformidade().getStatus()
        );
        assertEquals(NOVO_PRAZO, cenario.naoConformidade().getPrazoEm());

        ArgumentCaptor<EscalonamentoNcEntity> captor =
                ArgumentCaptor.forClass(EscalonamentoNcEntity.class);
        verify(escalonamentoRepository).saveAndFlush(captor.capture());
        assertEquals(NivelEscalonamento.N2, captor.getValue().getNivel());
        assertEquals(superior, captor.getValue().getResponsavel());

        ArgumentCaptor<NotificacaoEntity> notificacaoCaptor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository).save(notificacaoCaptor.capture());
        assertEquals(
                TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N2,
                notificacaoCaptor.getValue().getTipo()
        );
    }

    @Test
    void deveMarcarPrazoFinalN2ComoVencidoENotificarN2EAuditor() {
        Cenario cenario = cenario(
                StatusNaoConformidade.ESCALONADA_N2,
                AGORA.minusMinutes(1)
        );
        ParticipacaoPlanoEntity superior = superior(cenario.plano(), PapelPlano.SUPERIOR_N2);
        localizar(cenario);
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N2
        )).thenReturn(List.of(superior));

        boolean processada = service().processar(
                cenario.naoConformidade().getId(),
                AGORA
        );

        assertTrue(processada);
        assertEquals(
                StatusNaoConformidade.VENCIDA_N2,
                cenario.naoConformidade().getStatus()
        );

        ArgumentCaptor<NotificacaoEntity> captor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(notificacao ->
                notificacao.getTipo()
                        == TipoNotificacao.PRAZO_ESCALONAMENTO_N2_VENCIDO
        ));
        assertTrue(captor.getAllValues().stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(cenario.auditor())
        ));
        assertTrue(captor.getAllValues().stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(superior)
        ));
        verify(escalonamentoRepository, never()).saveAndFlush(any());
    }

    @Test
    void naoDeveProcessarNcComPrazoFuturo() {
        Cenario cenario = cenario(
                StatusNaoConformidade.EM_TRATAMENTO,
                AGORA.plusMinutes(1)
        );
        localizar(cenario);

        boolean processada = service().processar(
                cenario.naoConformidade().getId(),
                AGORA
        );

        assertFalse(processada);
        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void naoDeveProcessarNcAguardandoValidacao() {
        Cenario cenario = cenario(
                StatusNaoConformidade.RESOLUCAO_INFORMADA,
                AGORA.minusMinutes(1)
        );
        localizar(cenario);

        boolean processada = service().processar(
                cenario.naoConformidade().getId(),
                AGORA
        );

        assertFalse(processada);
        verify(escalonamentoRepository, never()).saveAndFlush(any());
        verify(naoConformidadeRepository, never()).save(any());
    }

    @Test
    void deveIgnorarNcRemovidaEntreConsultaEProcessamento() {
        UUID naoConformidadeId = UUID.randomUUID();
        when(naoConformidadeRepository.buscarPorIdParaAtualizacao(
                naoConformidadeId
        )).thenReturn(Optional.empty());

        boolean processada = service().processar(
                naoConformidadeId,
                AGORA
        );

        assertFalse(processada);
        verify(escalonamentoRepository, never()).saveAndFlush(any());
    }

    private void prepararEscalonamento(
            Cenario cenario,
            PapelPlano papel,
            ParticipacaoPlanoEntity superior
    ) {
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                papel
        )).thenReturn(List.of(superior));
        when(calendarioPrazoService.calcularPrazo(
                cenario.plano().getId(),
                AGORA,
                24
        )).thenReturn(NOVO_PRAZO);
    }

    private VencimentoNaoConformidadeService service() {
        return new VencimentoNaoConformidadeService(
                naoConformidadeRepository,
                notificacaoRepository,
                escalonamentoRepository,
                participacaoRepository,
                calendarioPrazoService
        );
    }

    private void localizar(Cenario cenario) {
        when(naoConformidadeRepository.buscarPorIdParaAtualizacao(
                cenario.naoConformidade().getId()
        )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private Cenario cenario(
            StatusNaoConformidade status,
            OffsetDateTime prazoEm
    ) {
        PlanoEntity plano = PlanoEntity.builder()
                .id(UUID.randomUUID())
                .build();
        ParticipacaoPlanoEntity auditor = ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .build();
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
                        .prazoEm(prazoEm)
                        .prazoResolucaoHoras(24)
                        .status(status)
                        .atualizadoEm(AGORA.minusDays(1))
                        .build();

        return new Cenario(plano, auditor, naoConformidade);
    }

    private ParticipacaoPlanoEntity superior(
            PlanoEntity plano,
            PapelPlano papel
    ) {
        ParticipacaoPlanoEntity superior = ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .build();
        superior.setPapel(papel);
        return superior;
    }

    private record Cenario(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor,
            NaoConformidadeEntity naoConformidade
    ) {
    }
}
