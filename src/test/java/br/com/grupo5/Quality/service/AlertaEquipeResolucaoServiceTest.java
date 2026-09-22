package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertaEquipeResolucaoServiceTest {

    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void superiorN1DeveAlertarTodaEquipeNoEscalonamentoN1() {
        UUID planoId = UUID.randomUUID();
        NaoConformidadeEntity nc = nc(StatusNaoConformidade.ESCALONADA_N1);
        ParticipacaoPlanoEntity superior = participante(PapelPlano.SUPERIOR_N1);
        ParticipacaoPlanoEntity membroUm =
                participante(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO);
        ParticipacaoPlanoEntity membroDois =
                participante(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO);
        when(acessoPlanoService.buscarParticipacao(auth, planoId))
                .thenReturn(superior);
        when(naoConformidadeRepository.buscarParaAtualizacao(nc.getId(), planoId))
                .thenReturn(Optional.of(nc));
        when(participacaoRepository.buscarPorPapelNoPlano(
                planoId,
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        )).thenReturn(List.of(membroUm, membroDois));

        int total = service().emitir(auth, planoId, nc.getId());

        assertEquals(2, total);
        ArgumentCaptor<NotificacaoEntity> captor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());
        assertEquals(
                List.of(membroUm, membroDois),
                captor.getAllValues().stream()
                        .map(NotificacaoEntity::getDestinatario)
                        .toList()
        );
        captor.getAllValues().forEach(notificacao -> assertEquals(
                TipoNotificacao.ALERTA_SUPERIOR_EQUIPE_RESOLUCAO,
                notificacao.getTipo()
        ));
    }

    @Test
    void superiorN2NaoDeveAlertarAntesDoEscalonamentoN2() {
        UUID planoId = UUID.randomUUID();
        NaoConformidadeEntity nc = nc(StatusNaoConformidade.ESCALONADA_N1);
        when(acessoPlanoService.buscarParticipacao(auth, planoId))
                .thenReturn(participante(PapelPlano.SUPERIOR_N2));
        when(naoConformidadeRepository.buscarParaAtualizacao(nc.getId(), planoId))
                .thenReturn(Optional.of(nc));

        assertThrows(
                InvalidRequestException.class,
                () -> service().emitir(auth, planoId, nc.getId())
        );

        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
    }

    @Test
    void auditorNaoDeveEmitirAlertaDeSuperior() {
        UUID planoId = UUID.randomUUID();
        when(acessoPlanoService.buscarParticipacao(auth, planoId))
                .thenReturn(participante(
                        PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
                ));

        assertThrows(
                AccessDeniedException.class,
                () -> service().emitir(auth, planoId, UUID.randomUUID())
        );

        verify(naoConformidadeRepository, never())
                .buscarParaAtualizacao(any(), any());
    }

    private AlertaEquipeResolucaoService service() {
        return new AlertaEquipeResolucaoService(
                naoConformidadeRepository,
                participacaoRepository,
                notificacaoRepository,
                acessoPlanoService
        );
    }

    private NaoConformidadeEntity nc(StatusNaoConformidade status) {
        return NaoConformidadeEntity.builder()
                .id(UUID.randomUUID())
                .status(status)
                .build();
    }

    private ParticipacaoPlanoEntity participante(PapelPlano papel) {
        ParticipacaoPlanoEntity participante =
                ParticipacaoPlanoEntity.builder()
                        .id(UUID.randomUUID())
                        .build();
        participante.setPapel(papel);
        return participante;
    }
}
