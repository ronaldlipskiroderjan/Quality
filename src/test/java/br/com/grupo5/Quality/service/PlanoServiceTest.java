package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class PlanoServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private PlanoRepository planoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;
    @Mock
    private Authentication auth;

    @Test
    void deveCriarPlanoPendenteEVincularAoUsuario() {
        UsuarioEntity usuario = usuario();
        when(auth.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(planoRepository.save(any(PlanoEntity.class)))
                .thenAnswer(invocation -> {
                    PlanoEntity plano = invocation.getArgument(0);
                    plano.setId(UUID.randomUUID());
                    return plano;
                });

        PlanoDetalhadoResponseDTO response = service().criar(auth, planoDto());

        ArgumentCaptor<PlanoEntity> captor = ArgumentCaptor.forClass(PlanoEntity.class);
        verify(planoRepository).save(captor.capture());
        PlanoEntity plano = captor.getValue();

        assertEquals("Quality", plano.getNomeProjeto());
        assertEquals(Status.PENDENTE, plano.getStatus());
        assertNotNull(plano.getCriadoEm());
        assertTrue(usuario.getPlanos().contains(plano));
        assertEquals(plano.getId(), response.id());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void naoDeveCriarPlanoSemUsuarioAutenticado() {
        when(auth.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service().criar(auth, planoDto()));
        verify(planoRepository, never()).save(any());
    }

    @Test
    void deveListarSomentePlanosDoUsuario() {
        PlanoEntity primeiro = plano("Projeto A", Status.PENDENTE);
        PlanoEntity segundo = plano("Projeto B", Status.CONCLUIDO);
        when(auth.getName()).thenReturn(EMAIL);
        when(planoRepository.findAllByUsuarioEmail(EMAIL))
                .thenReturn(List.of(primeiro, segundo));

        List<PlanoResponseDTO> response = service().listar(auth);

        assertEquals(2, response.size());
        assertEquals(primeiro.getId(), response.getFirst().id());
        verify(planoRepository).findAllByUsuarioEmail(EMAIL);
    }

    @Test
    void deveBuscarPlanoComObjetivoEVisaoGeral() {
        PlanoEntity plano = plano("Quality", Status.PENDENTE);
        when(acessoPlanoService.buscar(auth, plano.getId())).thenReturn(plano);

        PlanoDetalhadoResponseDTO response = service().buscar(auth, plano.getId());

        assertEquals(plano.getId(), response.id());
        assertEquals("Objetivo", response.objetivo());
        assertEquals("Visão geral", response.visaoGeral());
    }

    @Test
    void deveAtualizarPlanoAcessivel() {
        PlanoEntity plano = plano("Antigo", Status.PENDENTE);
        when(acessoPlanoService.buscar(auth, plano.getId())).thenReturn(plano);
        when(planoRepository.save(plano)).thenReturn(plano);

        PlanoDetalhadoResponseDTO response = service().atualizar(
                auth,
                plano.getId(),
                planoDto()
        );

        assertEquals("Quality", plano.getNomeProjeto());
        assertEquals("Garantir qualidade", plano.getObjetivo());
        assertEquals("Quality", response.nomeProjeto());
    }

    @Test
    void deveConcluirPlanoAcessivel() {
        PlanoEntity plano = plano("Quality", Status.PENDENTE);
        when(acessoPlanoService.buscar(auth, plano.getId())).thenReturn(plano);

        service().concluir(auth, plano.getId());

        assertEquals(Status.CONCLUIDO, plano.getStatus());
        verify(planoRepository).save(plano);
    }

    @Test
    void deveRemoverVinculosAntesDeExcluirPlano() {
        PlanoEntity plano = plano("Quality", Status.PENDENTE);
        UsuarioEntity primeiro = usuario();
        UsuarioEntity segundo = usuario();
        primeiro.getPlanos().add(plano);
        segundo.getPlanos().add(plano);

        when(acessoPlanoService.buscar(auth, plano.getId())).thenReturn(plano);
        when(usuarioRepository.findAllByPlanosId(plano.getId()))
                .thenReturn(List.of(primeiro, segundo));

        service().excluir(auth, plano.getId());

        assertFalse(primeiro.getPlanos().contains(plano));
        assertFalse(segundo.getPlanos().contains(plano));
        verify(usuarioRepository).saveAll(List.of(primeiro, segundo));
        verify(planoRepository).delete(plano);
    }

    private PlanoService service() {
        return new PlanoService(
                planoRepository,
                usuarioRepository,
                acessoPlanoService
        );
    }

    private PlanoRequestDTO planoDto() {
        return new PlanoRequestDTO(
                " Quality ",
                " 1.0 ",
                " Garantir qualidade ",
                " Visão geral "
        );
    }

    private PlanoEntity plano(String nome, Status status) {
        return PlanoEntity.builder()
                .id(UUID.randomUUID())
                .nomeProjeto(nome)
                .versao("1.0")
                .objetivo("Objetivo")
                .visaoGeral("Visão geral")
                .status(status)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .email(EMAIL)
                .planos(new HashSet<>())
                .build();
    }
}
