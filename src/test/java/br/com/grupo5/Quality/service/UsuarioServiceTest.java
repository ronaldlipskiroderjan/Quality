package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.apache.coyote.BadRequestException;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForCalendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PastOrPresentValidatorForCalendar pastOrPresentValidatorForCalendar;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private Authentication authentication;
    @Mock
    private MultipartFile multipartFile;

    @Test
    void deveSalvarImagemDePerfil() throws Exception {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        byte[] conteudo = {1, 2, 3};
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(multipartFile.getBytes()).thenReturn(conteudo);
        when(multipartFile.getContentType()).thenReturn("image/png");

        service.saveImagem(authentication, multipartFile);

        assertArrayEquals(conteudo, usuario.getFotoPerfil());
        assertEquals("image/png", usuario.getContentType());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveRetornarImagemDePerfil() throws Exception {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        usuario.setContentType("image/png");
        usuario.setFotoPerfil(new byte[]{1, 2, 3});
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        ImagemResponseDTO resultado = service.findImagem(authentication);

        assertEquals("image/png", resultado.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, resultado.imagem());
    }

    @Test
    void deveRetornarDadosDoUsuarioAutenticado() throws Exception {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        UsuarioResponseDTO resultado = service.findMe(authentication);

        assertEquals(new UsuarioResponseDTO("Usuario", EMAIL), resultado);
    }

    @Test
    void deveListarUsuariosPaginadosParaAdministracao() {
        UsuarioService service = service();
        var pageable = PageRequest.of(0, 15);
        UsuarioEntity usuario = usuario();
        Page<UsuarioEntity> pagina = new PageImpl<>(List.of(usuario), pageable, 1);
        when(usuarioRepository.findAll(pageable)).thenReturn(pagina);

        Page<UsuarioAdminResponseDTO> resultado = service.findAll(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(usuario.getId(), resultado.getContent().getFirst().id());
        assertEquals("Usuario", resultado.getContent().getFirst().nome());
        assertEquals(EMAIL, resultado.getContent().getFirst().email());
    }

    @Test
    void deveAtualizarSenhaQuandoSenhaAntigaConfereEConfirmacaoEValida() throws Exception {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        usuario.setSenhaHash("hash-antigo");
        PasswordRequestDTO dto = new PasswordRequestDTO("senha-antiga", "senha-nova", "senha-nova");
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-antiga", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");

        service.updatePassword(authentication, dto);

        assertEquals("hash-novo", usuario.getSenhaHash());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveRecusarSenhaAntigaIncorreta() {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        usuario.setSenhaHash("hash-antigo");
        PasswordRequestDTO dto = new PasswordRequestDTO("senha-incorreta", "senha-nova", "senha-nova");
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        assertThrows(BadRequestException.class, () -> service.updatePassword(authentication, dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRecusarConfirmacaoDeSenhaDiferente() {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        usuario.setSenhaHash("hash-antigo");
        PasswordRequestDTO dto = new PasswordRequestDTO("senha-antiga", "senha-nova", "outra-senha");
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha-antiga", "hash-antigo")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.updatePassword(authentication, dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveAtualizarNomeEEmailDoUsuario() throws Exception {
        UsuarioService service = service();
        UsuarioEntity usuario = usuario();
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO("Novo nome", "novo@quality.com");
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        service.updateMe(authentication, dto);

        assertEquals("Novo nome", usuario.getNome());
        assertEquals("novo@quality.com", usuario.getEmail());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveInformarQuandoUsuarioAutenticadoNaoExiste() {
        UsuarioService service = service();
        when(authentication.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findMe(authentication));
    }

    private UsuarioService service() {
        return new UsuarioService(usuarioRepository, pastOrPresentValidatorForCalendar, passwordEncoder);
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nome("Usuario")
                .email(EMAIL)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
