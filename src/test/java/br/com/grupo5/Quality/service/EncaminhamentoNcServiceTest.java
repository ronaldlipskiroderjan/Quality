package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.EncaminhamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EncaminhamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.EncaminhamentoResultado;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EncaminhamentoNcServiceTest {

    @Mock
    private EncaminhamentoNcRepository encaminhamentoRepository;
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
    void deveEncaminharParaTodosOsMembrosDaEquipe() {
        Cenario cenario = cenario();
        ParticipacaoPlanoEntity segundoMembro = membroEquipe("Segundo membro");
        localizarNaoConformidade(cenario);
        autorizarAuditor(cenario);
        when(encaminhamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        "chave-1"
                )).thenReturn(Optional.empty());
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.planoId(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        )).thenReturn(List.of(cenario.responsavel(), segundoMembro));
        when(encaminhamentoRepository.saveAndFlush(
                any(EncaminhamentoNcEntity.class)
        )).thenAnswer(invocation -> {
            EncaminhamentoNcEntity encaminhamento = invocation.getArgument(0);
            encaminhamento.setId(UUID.randomUUID());
            return encaminhamento;
        });
        when(calendarioPrazoService.calcularPrazo(
                any(), any(), org.mockito.ArgumentMatchers.anyInt()
        )).thenAnswer(invocation -> ((OffsetDateTime) invocation.getArgument(1))
                .plusHours(((Integer) invocation.getArgument(2)).longValue()));

        EncaminhamentoResultado resultado = service().encaminhar(
                auth,
                cenario.planoId(),
                cenario.naoConformidade().getId(),
                " chave-1 "
        );

        assertTrue(resultado.criado());
        assertEquals(2, resultado.encaminhamento().totalDestinatarios());
        assertEquals(
                StatusNaoConformidade.EM_TRATAMENTO,
                cenario.naoConformidade().getStatus()
        );
        assertNotNull(cenario.naoConformidade().getEnviadaEm());
        assertEquals(
                cenario.naoConformidade().getEnviadaEm().plusHours(24),
                cenario.naoConformidade().getPrazoEm()
        );

        ArgumentCaptor<NotificacaoEntity> captor =
                ArgumentCaptor.forClass(NotificacaoEntity.class);
        verify(notificacaoRepository, org.mockito.Mockito.times(2))
                .save(captor.capture());
        assertTrue(captor.getAllValues().stream().allMatch(notificacao ->
                notificacao.getTipo()
                        == TipoNotificacao.NAO_CONFORMIDADE_ENCAMINHADA_EQUIPE
        ));
        verify(naoConformidadeRepository)
                .save(cenario.naoConformidade());
    }

    @Test
    void deveRetornarEncaminhamentoExistenteParaMesmaChave() {
        Cenario cenario = cenario();
        EncaminhamentoNcEntity existente = EncaminhamentoNcEntity.builder()
                .id(UUID.randomUUID())
                .naoConformidade(cenario.naoConformidade())
                .chaveIdempotencia("repetida")
                .totalDestinatarios(1)
                .encaminhadoEm(OffsetDateTime.now())
                .build();
        localizarNaoConformidade(cenario);
        autorizarAuditor(cenario);
        when(encaminhamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        "repetida"
                )).thenReturn(Optional.of(existente));

        EncaminhamentoResultado resultado = service().encaminhar(
                auth,
                cenario.planoId(),
                cenario.naoConformidade().getId(),
                "repetida"
        );

        assertFalse(resultado.criado());
        assertEquals(existente.getId(), resultado.encaminhamento().id());
        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void naoDeveEncaminharSemEquipeDeResolucao() {
        Cenario cenario = cenario();
        localizarNaoConformidade(cenario);
        autorizarAuditor(cenario);
        when(encaminhamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        "sem-equipe"
                )).thenReturn(Optional.empty());
        when(participacaoRepository.buscarPorPapelNoPlano(
                cenario.planoId(),
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        )).thenReturn(List.of());

        assertThrows(
                InvalidRequestException.class,
                () -> service().encaminhar(
                        auth,
                        cenario.planoId(),
                        cenario.naoConformidade().getId(),
                        "sem-equipe"
                )
        );

        verify(encaminhamentoRepository, never()).saveAndFlush(any());
        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void naoDeveEncaminharSemAcaoCorretiva() {
        Cenario cenario = cenario();
        cenario.naoConformidade().setAcaoCorretiva(" ");
        localizarNaoConformidade(cenario);
        autorizarAuditor(cenario);
        when(encaminhamentoRepository
                .findByNaoConformidadeIdAndChaveIdempotencia(
                        cenario.naoConformidade().getId(),
                        "sem-acao"
                )).thenReturn(Optional.empty());

        assertThrows(
                InvalidRequestException.class,
                () -> service().encaminhar(
                        auth,
                        cenario.planoId(),
                        cenario.naoConformidade().getId(),
                        "sem-acao"
                )
        );

        verify(participacaoRepository, never())
                .buscarPorPapelNoPlano(any(), any());
    }

    private void localizarNaoConformidade(Cenario cenario) {
        when(naoConformidadeRepository.buscarParaAtualizacao(
                cenario.naoConformidade().getId(),
                cenario.planoId()
        )).thenReturn(Optional.of(cenario.naoConformidade()));
    }

    private void autorizarAuditor(Cenario cenario) {
        when(acessoPlanoService.buscarParticipacao(
                auth,
                cenario.planoId(),
                PermissaoPlano.AUDITAR
        )).thenReturn(cenario.auditor());
    }

    private Cenario cenario() {
        UUID planoId = UUID.randomUUID();
        ParticipacaoPlanoEntity auditor = ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .build();
        ParticipacaoPlanoEntity responsavel = membroEquipe("Responsável");
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
                        .acaoCorretiva("Corrigir o item.")
                        .status(StatusNaoConformidade.RASCUNHO)
                        .prazoResolucaoHoras(24)
                        .build();
        return new Cenario(planoId, auditor, responsavel, naoConformidade);
    }

    private ParticipacaoPlanoEntity membroEquipe(String nome) {
        ParticipacaoPlanoEntity membro = ParticipacaoPlanoEntity.builder()
                .id(UUID.randomUUID())
                .usuario(UsuarioEntity.builder()
                        .id(UUID.randomUUID())
                        .nome(nome)
                        .build())
                .build();
        membro.setPapel(PapelPlano.MEMBRO_EQUIPE_RESOLUCAO);
        return membro;
    }

    private EncaminhamentoNcService service() {
        return new EncaminhamentoNcService(
                encaminhamentoRepository,
                naoConformidadeRepository,
                participacaoRepository,
                notificacaoRepository,
                acessoPlanoService,
                calendarioPrazoService
        );
    }

    private record Cenario(
            UUID planoId,
            ParticipacaoPlanoEntity auditor,
            ParticipacaoPlanoEntity responsavel,
            NaoConformidadeEntity naoConformidade
    ) {
    }
}
