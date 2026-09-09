package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoPlanoServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private ParticipacaoPlanoRepository participacaoRepository;
    @Mock
    private Authentication auth;

    @Test
    void deveRetornarParticipacaoDoUsuario() {
        ParticipacaoPlanoEntity participacao = participacao(PapelPlano.PARTICIPANTE);
        UUID planoId = participacao.getPlano().getId();
        when(auth.getName()).thenReturn(EMAIL);
        when(participacaoRepository.findByPlanoIdAndUsuarioEmailIgnoreCase(
                planoId,
                EMAIL
        )).thenReturn(Optional.of(participacao));

        ParticipacaoPlanoEntity response = service().buscarParticipacao(
                auth,
                planoId
        );

        assertSame(participacao, response);
    }

    @Test
    void naoDeveExporPlanoSemParticipacao() {
        UUID planoId = UUID.randomUUID();
        when(auth.getName()).thenReturn(EMAIL);
        when(participacaoRepository.findByPlanoIdAndUsuarioEmailIgnoreCase(
                planoId,
                EMAIL
        )).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service().buscarPlano(auth, planoId)
        );
    }

    @Test
    void devePermitirOperacaoAutorizadaPeloPapel() {
        ParticipacaoPlanoEntity participacao = participacao(
                PapelPlano.RESPONSAVEL_QUALIDADE
        );
        UUID planoId = participacao.getPlano().getId();
        when(auth.getName()).thenReturn(EMAIL);
        when(participacaoRepository.findByPlanoIdAndUsuarioEmailIgnoreCase(
                planoId,
                EMAIL
        )).thenReturn(Optional.of(participacao));

        PlanoEntity response = service().buscarPlano(
                auth,
                planoId,
                PermissaoPlano.EDITAR
        );

        assertSame(participacao.getPlano(), response);
    }

    @Test
    void deveNegarOperacaoSemPermissaoContextual() {
        ParticipacaoPlanoEntity participacao = participacao(PapelPlano.AUDITOR);
        UUID planoId = participacao.getPlano().getId();
        when(auth.getName()).thenReturn(EMAIL);
        when(participacaoRepository.findByPlanoIdAndUsuarioEmailIgnoreCase(
                planoId,
                EMAIL
        )).thenReturn(Optional.of(participacao));

        assertThrows(
                AccessDeniedException.class,
                () -> service().buscarPlano(
                        auth,
                        planoId,
                        PermissaoPlano.EDITAR
                )
        );
    }

    private AcessoPlanoService service() {
        return new AcessoPlanoService(participacaoRepository);
    }

    private ParticipacaoPlanoEntity participacao(PapelPlano papel) {
        return ParticipacaoPlanoEntity.builder()
                .plano(PlanoEntity.builder().id(UUID.randomUUID()).build())
                .papeis(EnumSet.of(papel))
                .build();
    }
}
