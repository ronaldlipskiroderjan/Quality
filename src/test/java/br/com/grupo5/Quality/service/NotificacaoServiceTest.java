package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.dto.response.NotificacaoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    private static final String EMAIL = "auditor@quality.com";

    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private Authentication auth;

    @Test
    void deveListarSomenteNotificacoesDoUsuario() {
        NotificacaoEntity notificacao = notificacao(
                StatusNotificacao.NAO_LIDA
        );
        when(auth.getName()).thenReturn(EMAIL);
        PageRequest pageable = PageRequest.of(0, 15);
        when(notificacaoRepository.findAllByDestinatarioUsuarioEmailIgnoreCase(
                EMAIL,
                pageable
        )).thenReturn(new PageImpl<>(List.of(notificacao), pageable, 1));

        PaginaResponseDTO<NotificacaoResponseDTO> response = service().listar(auth, pageable);

        assertEquals(1, response.totalElementos());
        assertEquals(notificacao.getId(), response.conteudo().getFirst().id());
        assertEquals(
                notificacao.getDestinatario().getPlano().getId(),
                response.conteudo().getFirst().planoId()
        );
        assertEquals(
                notificacao.getNaoConformidade().getId(),
                response.conteudo().getFirst().naoConformidadeId()
        );
    }

    @Test
    void deveMarcarNotificacaoComoLida() {
        NotificacaoEntity notificacao = notificacao(
                StatusNotificacao.NAO_LIDA
        );
        localizar(notificacao);
        when(notificacaoRepository.save(any(NotificacaoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificacaoResponseDTO response = service().marcarComoLida(
                auth,
                notificacao.getId()
        );

        assertEquals(StatusNotificacao.LIDA, response.status());
        assertNotNull(response.lidaEm());
        verify(notificacaoRepository).save(notificacao);
    }

    @Test
    void leituraRepetidaDeveSerIdempotente() {
        NotificacaoEntity notificacao = notificacao(
                StatusNotificacao.LIDA
        );
        notificacao.setLidaEm(OffsetDateTime.now(ZoneOffset.UTC));
        localizar(notificacao);

        NotificacaoResponseDTO response = service().marcarComoLida(
                auth,
                notificacao.getId()
        );

        assertTrue(notificacao.lida());
        assertEquals(notificacao.getLidaEm(), response.lidaEm());
        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void naoDevePermitirLerNotificacaoDeOutroUsuario() {
        UUID notificacaoId = UUID.randomUUID();
        when(auth.getName()).thenReturn(EMAIL);
        when(notificacaoRepository
                .findByIdAndDestinatarioUsuarioEmailIgnoreCase(
                        notificacaoId,
                        EMAIL
                )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service().marcarComoLida(auth, notificacaoId)
        );
    }

    private NotificacaoService service() {
        return new NotificacaoService(notificacaoRepository);
    }

    private void localizar(NotificacaoEntity notificacao) {
        when(auth.getName()).thenReturn(EMAIL);
        when(notificacaoRepository
                .findByIdAndDestinatarioUsuarioEmailIgnoreCase(
                        notificacao.getId(),
                        EMAIL
                )).thenReturn(Optional.of(notificacao));
    }

    private NotificacaoEntity notificacao(StatusNotificacao status) {
        PlanoEntity plano = PlanoEntity.builder()
                .id(UUID.randomUUID())
                .build();
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .build();
        ParticipacaoPlanoEntity destinatario =
                ParticipacaoPlanoEntity.builder()
                        .id(UUID.randomUUID())
                        .plano(plano)
                        .usuario(usuario)
                        .build();
        NaoConformidadeEntity naoConformidade =
                NaoConformidadeEntity.builder()
                        .id(UUID.randomUUID())
                        .build();

        return NotificacaoEntity.builder()
                .id(UUID.randomUUID())
                .destinatario(destinatario)
                .naoConformidade(naoConformidade)
                .chaveEvento("evento:" + UUID.randomUUID())
                .tipo(TipoNotificacao.PRAZO_NAO_CONFORMIDADE_VENCIDO)
                .titulo("Prazo vencido")
                .mensagem("Mensagem")
                .status(status)
                .criadaEm(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}
