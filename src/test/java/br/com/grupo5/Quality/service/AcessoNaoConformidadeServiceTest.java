package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.EncaminhamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoNaoConformidadeServiceTest {

    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private EncaminhamentoNcRepository encaminhamentoRepository;
    @Mock
    private Authentication auth;

    @Test
    void devePermitirEquipeQuandoNcFoiEncaminhada() {
        Cenario cenario = cenario(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO);
        preparar(cenario);
        when(encaminhamentoRepository.existsByNaoConformidadeId(
                cenario.naoConformidadeId()
        )).thenReturn(true);

        assertDoesNotThrow(() -> service().validarConsulta(
                auth,
                cenario.planoId(),
                cenario.naoConformidadeId()
        ));
    }

    @Test
    void naoDevePermitirEquipeQuandoNcNaoFoiEncaminhada() {
        Cenario cenario = cenario(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO);
        preparar(cenario);
        when(encaminhamentoRepository.existsByNaoConformidadeId(
                cenario.naoConformidadeId()
        )).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service().validarConsulta(
                        auth,
                        cenario.planoId(),
                        cenario.naoConformidadeId()
                )
        );
    }

    @Test
    void devePermitirSuperiorSomenteQuandoNotificado() {
        Cenario cenario = cenario(PapelPlano.SUPERIOR_N1);
        preparar(cenario);
        when(notificacaoRepository
                .existsByNaoConformidadeIdAndDestinatarioId(
                        cenario.naoConformidadeId(),
                        cenario.participacao().getId()
                )).thenReturn(true);

        assertDoesNotThrow(() -> service().validarConsulta(
                auth,
                cenario.planoId(),
                cenario.naoConformidadeId()
        ));
    }

    private void preparar(Cenario cenario) {
        when(auth.getName()).thenReturn("usuario@quality.test");
        when(participacaoRepository
                .findByPlanoIdAndUsuarioEmailIgnoreCase(
                        cenario.planoId(),
                        "usuario@quality.test"
                )).thenReturn(Optional.of(cenario.participacao()));
        when(naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        cenario.naoConformidadeId(),
                        cenario.planoId()
                )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private Cenario cenario(PapelPlano papel) {
        UUID planoId = UUID.randomUUID();
        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .id(UUID.randomUUID())
                        .status(StatusNaoConformidade.ENVIADA)
                        .build();
        ParticipacaoPlanoEntity participacao =
                ParticipacaoPlanoEntity.builder()
                        .id(UUID.randomUUID())
                        .build();
        participacao.setPapel(papel);
        return new Cenario(planoId, participacao, naoConformidade);
    }

    private AcessoNaoConformidadeService service() {
        return new AcessoNaoConformidadeService(
                participacaoRepository,
                notificacaoRepository,
                naoConformidadeRepository,
                encaminhamentoRepository
        );
    }

    private record Cenario(
            UUID planoId,
            ParticipacaoPlanoEntity participacao,
            NaoConformidadeEntity naoConformidade
    ) {
        private UUID naoConformidadeId() {
            return naoConformidade.getId();
        }
    }
}
