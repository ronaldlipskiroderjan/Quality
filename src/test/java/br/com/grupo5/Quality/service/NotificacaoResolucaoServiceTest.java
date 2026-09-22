package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
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
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoResolucaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private EscalonamentoNcRepository escalonamentoRepository;

    @Test
    void deveNotificarTodosOsAuditoresSemEscalonamento() {
        Cenario cenario = cenario();
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )).thenReturn(List.of(cenario.auditor(), cenario.outroAuditor()));
        when(escalonamentoRepository.findAllByNaoConformidadeId(
                cenario.naoConformidade().getId()
        )).thenReturn(List.of());
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(false);

        service().notificarResolucaoInformada(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        List<NotificacaoEntity> notificacoes = notificacoesSalvas();
        assertEquals(2, notificacoes.size());
        assertTrue(notificacoes.stream().allMatch(notificacao ->
                notificacao.getTipo() == TipoNotificacao.RESOLUCAO_INFORMADA
                        && notificacao.getStatus()
                                == StatusNotificacao.NAO_LIDA
                        && notificacao.getCriadaEm().equals(cenario.agora())));
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(cenario.auditor())));
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(
                        cenario.outroAuditor())));
        verify(participacaoRepository, never()).buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        );
        verify(participacaoRepository, never()).buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N2
        );
    }

    @Test
    void deveNotificarAuditoresESuperiorN1QuandoEscalonadaEmN1() {
        Cenario cenario = cenario();
        prepararAuditores(cenario);
        when(escalonamentoRepository.findAllByNaoConformidadeId(
                cenario.naoConformidade().getId()
        )).thenReturn(List.of(escalonamento(NivelEscalonamento.N1)));
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(cenario.superiorN1()));
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(false);

        service().notificarResolucaoInformada(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        List<NotificacaoEntity> notificacoes = notificacoesSalvas();
        assertEquals(2, notificacoes.size());
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(
                        cenario.superiorN1())));
        verify(participacaoRepository, never()).buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N2
        );
    }

    @Test
    void deveNotificarAuditoresESuperioresN1EN2QuandoEscalonadaEmN2() {
        Cenario cenario = cenario();
        prepararAuditores(cenario);
        when(escalonamentoRepository.findAllByNaoConformidadeId(
                cenario.naoConformidade().getId()
        )).thenReturn(List.of(escalonamento(NivelEscalonamento.N2)));
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N1
        )).thenReturn(List.of(cenario.superiorN1()));
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.SUPERIOR_N2
        )).thenReturn(List.of(cenario.superiorN2()));
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(false);

        service().notificarResolucaoInformada(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        List<NotificacaoEntity> notificacoes = notificacoesSalvas();
        assertEquals(3, notificacoes.size());
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(
                        cenario.superiorN1())));
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(
                        cenario.superiorN2())));
    }

    @Test
    void naoDeveDuplicarNotificacaoDoMesmoEvento() {
        Cenario cenario = cenario();
        prepararAuditores(cenario);
        when(escalonamentoRepository.findAllByNaoConformidadeId(
                cenario.naoConformidade().getId()
        )).thenReturn(List.of());
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(true);

        service().notificarResolucaoInformada(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        verify(notificacaoRepository, never()).saveAll(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void deveNotificarResponsavelAtribuidoQuandoAjustesForemSolicitados() {
        Cenario cenario = cenario();
        cenario.naoConformidade().setResponsavel(cenario.responsavel());
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(false);

        service().notificarAjustesSolicitados(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        List<NotificacaoEntity> notificacoes = notificacoesSalvas();
        assertEquals(1, notificacoes.size());
        NotificacaoEntity notificacao = notificacoes.getFirst();
        assertEquals(cenario.responsavel(), notificacao.getDestinatario());
        assertEquals(
                TipoNotificacao.AJUSTES_RESOLUCAO_SOLICITADOS,
                notificacao.getTipo()
        );
        assertEquals(StatusNotificacao.NAO_LIDA, notificacao.getStatus());
        assertEquals(cenario.agora(), notificacao.getCriadaEm());
        verify(participacaoRepository, never()).buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        );
    }

    @Test
    void deveNotificarEquipeQuandoNaoHouverResponsavelAtribuido() {
        Cenario cenario = cenario();
        ParticipacaoPlanoEntity outroMembro = participante(
                cenario.plano(),
                "Outro membro"
        );
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        )).thenReturn(List.of(cenario.responsavel(), outroMembro));
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(false);

        service().notificarAjustesSolicitados(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        List<NotificacaoEntity> notificacoes = notificacoesSalvas();
        assertEquals(2, notificacoes.size());
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(cenario.responsavel())));
        assertTrue(notificacoes.stream().anyMatch(notificacao ->
                notificacao.getDestinatario().equals(outroMembro)));
    }

    @Test
    void naoDeveDuplicarNotificacaoDeAjustesDaMesmaResolucao() {
        Cenario cenario = cenario();
        cenario.naoConformidade().setResponsavel(cenario.responsavel());
        when(notificacaoRepository.existsByChaveEvento(anyString()))
                .thenReturn(true);

        service().notificarAjustesSolicitados(
                cenario.plano().getId(),
                cenario.naoConformidade(),
                cenario.resolucao(),
                cenario.agora()
        );

        verify(notificacaoRepository, never()).saveAll(
                org.mockito.ArgumentMatchers.any()
        );
    }

    private void prepararAuditores(Cenario cenario) {
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.plano().getId(),
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        )).thenReturn(List.of(cenario.auditor()));
    }

    private EscalonamentoNcEntity escalonamento(NivelEscalonamento nivel) {
        return EscalonamentoNcEntity.builder()
                .id(UUID.randomUUID())
                .nivel(nivel)
                .build();
    }

    private NotificacaoResolucaoService service() {
        return new NotificacaoResolucaoService(
                notificacaoRepository,
                participacaoRepository,
                escalonamentoRepository
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<NotificacaoEntity> notificacoesSalvas() {
        ArgumentCaptor<Iterable<NotificacaoEntity>> captor =
                ArgumentCaptor.forClass(Iterable.class);
        verify(notificacaoRepository).saveAll(captor.capture());
        return StreamSupport.stream(
                captor.getValue().spliterator(),
                false
        ).toList();
    }

    private Cenario cenario() {
        PlanoEntity plano = PlanoEntity.builder().id(UUID.randomUUID()).build();
        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .id(UUID.randomUUID())
                        .build();
        ResolucaoNcEntity resolucao = ResolucaoNcEntity.builder()
                .id(UUID.randomUUID())
                .naoConformidade(naoConformidade)
                .build();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        return new Cenario(
                plano,
                participante(plano, "Auditor"),
                participante(plano, "Outro auditor"),
                participante(plano, "Superior N1"),
                participante(plano, "Superior N2"),
                participante(plano, "Responsável"),
                naoConformidade,
                resolucao,
                agora
        );
    }

    private ParticipacaoPlanoEntity participante(
            PlanoEntity plano,
            String nome
    ) {
        return ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .plano(plano)
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome(nome)
                        .build())
                .build();
    }

    private record Cenario(
            PlanoEntity plano,
            ParticipacaoPlanoEntity auditor,
            ParticipacaoPlanoEntity outroAuditor,
            ParticipacaoPlanoEntity superiorN1,
            ParticipacaoPlanoEntity superiorN2,
            ParticipacaoPlanoEntity responsavel,
            NaoConformidadeEntity naoConformidade,
            ResolucaoNcEntity resolucao,
            OffsetDateTime agora
    ) {
    }
}
