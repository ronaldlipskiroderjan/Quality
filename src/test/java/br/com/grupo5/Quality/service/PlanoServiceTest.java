package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
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
    private Authentication authentication;

    @Test
    void deveCriarPlanoPendenteEAdicionarAoUsuarioSemPerderPlanosAnteriores() throws Exception {
        PlanoService service = service();
        PlanoRequestDTO dto = planoDto();
        PlanoEntity planoAnterior = plano(UUID.randomUUID(), "Projeto anterior", Status.CONCLUIDO);
        UsuarioEntity usuario = UsuarioEntity.builder()
                .email(EMAIL)
                .planos(new HashSet<>(Set.of(planoAnterior)))
                .build();
        when(authentication.getName()).thenReturn(EMAIL);
        when(planoRepository.save(any(PlanoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        service.create(authentication, dto);

        ArgumentCaptor<PlanoEntity> captor = ArgumentCaptor.forClass(PlanoEntity.class);
        verify(planoRepository).save(captor.capture());
        PlanoEntity novoPlano = captor.getValue();
        assertEquals("Quality", novoPlano.getNomeProjeto());
        assertEquals(Status.PENDENTE, novoPlano.getStatus());
        assertNotNull(novoPlano.getCriadoEm());
        assertTrue(usuario.getPlanos().contains(planoAnterior));
        assertTrue(usuario.getPlanos().contains(novoPlano));
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void naoDeveSalvarPlanoQuandoUsuarioAutenticadoNaoExiste() {
        PlanoService service = service();
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.create(authentication, planoDto()));
        verify(planoRepository, never()).save(any());
    }

    @Test
    void deveListarPlanosDoUsuarioComoDto() throws Exception {
        PlanoService service = service();
        UUID usuarioId = UUID.randomUUID();
        PlanoEntity primeiro = plano(UUID.randomUUID(), "Projeto A", Status.PENDENTE);
        PlanoEntity segundo = plano(UUID.randomUUID(), "Projeto B", Status.CONCLUIDO);
        UsuarioEntity usuario = UsuarioEntity.builder().planos(Set.of(primeiro, segundo)).build();
        when(usuarioRepository.findByIdWithPlanos(usuarioId)).thenReturn(Optional.of(usuario));

        List<PlanoResponseDTO> resultado = service.findAll(usuarioId);

        assertEquals(2, resultado.size());
        assertTrue(resultado.contains(new PlanoResponseDTO(primeiro.getId(), "Projeto A", Status.PENDENTE)));
        assertTrue(resultado.contains(new PlanoResponseDTO(segundo.getId(), "Projeto B", Status.CONCLUIDO)));
    }

    @Test
    void deveBuscarPlanoPorId() throws Exception {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        when(planoRepository.findById(id)).thenReturn(Optional.of(plano(id, "Quality", Status.PENDENTE)));

        PlanoResponseDTO resultado = service.findById(id);

        assertEquals(new PlanoResponseDTO(id, "Quality", Status.PENDENTE), resultado);
    }

    @Test
    void deveInformarQuandoPlanoNaoExisteNaBusca() {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        when(planoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findById(id));
    }

    @Test
    void deveAtualizarDadosDoPlano() throws Exception {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        PlanoEntity plano = plano(id, "Antigo", Status.PENDENTE);
        when(planoRepository.findById(id)).thenReturn(Optional.of(plano));

        service.update(id, planoDto());

        assertEquals("Quality", plano.getNomeProjeto());
        assertEquals("1.0", plano.getVersao());
        assertEquals("Garantir qualidade", plano.getObjetivo());
        assertEquals("Visao geral", plano.getVisaoGeral());
        verify(planoRepository).save(plano);
    }

    @Test
    void deveConcluirPlano() throws Exception {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        PlanoEntity plano = plano(id, "Quality", Status.PENDENTE);
        when(planoRepository.findById(id)).thenReturn(Optional.of(plano));

        service.closePlano(id);

        assertEquals(Status.CONCLUIDO, plano.getStatus());
        verify(planoRepository).save(plano);
    }

    @Test
    void deveExcluirPlanoExistente() throws Exception {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        when(planoRepository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(planoRepository).deleteById(id);
    }

    @Test
    void naoDeveExcluirPlanoInexistente() {
        PlanoService service = service();
        UUID id = UUID.randomUUID();
        when(planoRepository.existsById(id)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.delete(id));
        verify(planoRepository, never()).deleteById(any());
    }

    private PlanoService service() {
        return new PlanoService(planoRepository, usuarioRepository);
    }

    private PlanoRequestDTO planoDto() {
        return new PlanoRequestDTO(
                "Usuario",
                "Quality",
                "1.0",
                "Garantir qualidade",
                "Visao geral"
        );
    }

    private PlanoEntity plano(UUID id, String nome, Status status) {
        return PlanoEntity.builder()
                .id(id)
                .nomeProjeto(nome)
                .versao("1.0")
                .objetivo("Objetivo")
                .visaoGeral("Visao geral")
                .status(status)
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
