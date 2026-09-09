package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcessoPlanoServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private PlanoRepository planoRepository;
    @Mock
    private Authentication auth;

    @Test
    void deveRetornarPlanoVinculadoAoUsuario() {
        UUID planoId = UUID.randomUUID();
        PlanoEntity plano = PlanoEntity.builder().id(planoId).build();
        when(auth.getName()).thenReturn(EMAIL);
        when(planoRepository.findByIdAndUsuarioEmail(planoId, EMAIL))
                .thenReturn(Optional.of(plano));

        PlanoEntity response = service().buscar(auth, planoId);

        assertSame(plano, response);
    }

    @Test
    void naoDeveExporPlanoSemVinculoComUsuario() {
        UUID planoId = UUID.randomUUID();
        when(auth.getName()).thenReturn(EMAIL);
        when(planoRepository.findByIdAndUsuarioEmail(planoId, EMAIL))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().buscar(auth, planoId));
    }

    private AcessoPlanoService service() {
        return new AcessoPlanoService(planoRepository);
    }
}
