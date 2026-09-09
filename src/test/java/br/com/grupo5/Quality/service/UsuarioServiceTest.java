package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
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
    private PasswordEncoder passwordEncoder;
    @Mock
    private Authentication auth;
    @Mock
    private MultipartFile imagem;

    @Test
    void deveSalvarImagemValida() throws Exception {
        UsuarioEntity usuario = usuario();
        byte[] conteudo = {1, 2, 3};

        prepararUsuario(usuario);
        when(imagem.isEmpty()).thenReturn(false);
        when(imagem.getContentType()).thenReturn("image/png");
        when(imagem.getBytes()).thenReturn(conteudo);

        service().salvarImagem(auth, imagem);

        assertArrayEquals(conteudo, usuario.getFotoPerfil());
        assertEquals("image/png", usuario.getTipoImagem());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveRecusarArquivoQueNaoSejaImagem() {
        when(imagem.isEmpty()).thenReturn(false);
        when(imagem.getContentType()).thenReturn("application/pdf");

        assertThrows(
                InvalidRequestException.class,
                () -> service().salvarImagem(auth, imagem)
        );
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRetornarImagemDePerfil() {
        UsuarioEntity usuario = usuario();
        usuario.setTipoImagem("image/png");
        usuario.setFotoPerfil(new byte[]{1, 2, 3});
        prepararUsuario(usuario);

        ImagemResponseDTO response = service().buscarImagem(auth);

        assertEquals("image/png", response.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, response.imagem());
    }

    @Test
    void deveInformarImagemAusente() {
        prepararUsuario(usuario());

        assertThrows(NotFoundException.class, () -> service().buscarImagem(auth));
    }

    @Test
    void deveRetornarPerfilDoUsuario() {
        UsuarioEntity usuario = usuario();
        prepararUsuario(usuario);

        UsuarioResponseDTO response = service().buscarPerfil(auth);

        assertEquals(usuario.getId(), response.id());
        assertEquals("Usuário", response.nome());
        assertEquals(EMAIL, response.email());
    }

    @Test
    void deveListarUsuariosParaAdministracao() {
        PageRequest pageable = PageRequest.of(0, 15);
        UsuarioEntity usuario = usuario();
        Page<UsuarioEntity> pagina = new PageImpl<>(List.of(usuario), pageable, 1);
        when(usuarioRepository.findAll(pageable)).thenReturn(pagina);

        Page<UsuarioAdminResponseDTO> response = service().listar(pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals(usuario.getId(), response.getContent().getFirst().id());
    }

    @Test
    void deveAlterarSenhaValida() {
        UsuarioEntity usuario = usuario();
        usuario.setSenhaHash("hash-antigo");
        PasswordRequestDTO dto = new PasswordRequestDTO(
                "senha-antiga",
                "senha-nova",
                "senha-nova"
        );

        prepararUsuario(usuario);
        when(passwordEncoder.matches("senha-antiga", "hash-antigo")).thenReturn(true);
        when(passwordEncoder.encode("senha-nova")).thenReturn("hash-novo");

        service().alterarSenha(auth, dto);

        assertEquals("hash-novo", usuario.getSenhaHash());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void deveRecusarSenhaAtualIncorreta() {
        UsuarioEntity usuario = usuario();
        usuario.setSenhaHash("hash-antigo");
        PasswordRequestDTO dto = new PasswordRequestDTO(
                "senha-incorreta",
                "senha-nova",
                "senha-nova"
        );

        prepararUsuario(usuario);
        when(passwordEncoder.matches("senha-incorreta", "hash-antigo")).thenReturn(false);

        assertThrows(
                InvalidRequestException.class,
                () -> service().alterarSenha(auth, dto)
        );
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRecusarConfirmacaoDiferente() {
        UsuarioEntity usuario = usuario();
        PasswordRequestDTO dto = new PasswordRequestDTO(
                "senha-antiga",
                "senha-nova",
                "outra-senha"
        );
        prepararUsuario(usuario);

        assertThrows(
                InvalidRequestException.class,
                () -> service().alterarSenha(auth, dto)
        );
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveAtualizarNomeEEmailNormalizado() {
        UsuarioEntity usuario = usuario();
        prepararUsuario(usuario);
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                " Novo nome ",
                " NOVO@QUALITY.COM "
        );
        when(usuarioRepository.existsByEmailIgnoreCase("novo@quality.com"))
                .thenReturn(false);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioResponseDTO response = service().atualizarPerfil(auth, dto);

        assertEquals("Novo nome", usuario.getNome());
        assertEquals("novo@quality.com", response.email());
    }

    @Test
    void deveRecusarEmailEmUsoPorOutroUsuario() {
        UsuarioEntity usuario = usuario();
        prepararUsuario(usuario);
        UsuarioUpdateRequestDTO dto = new UsuarioUpdateRequestDTO(
                "Usuário",
                "outro@quality.com"
        );
        when(usuarioRepository.existsByEmailIgnoreCase("outro@quality.com"))
                .thenReturn(true);

        assertThrows(
                AlreadyExistsException.class,
                () -> service().atualizarPerfil(auth, dto)
        );
        verify(usuarioRepository, never()).save(any());
    }

    private UsuarioService service() {
        return new UsuarioService(usuarioRepository, passwordEncoder);
    }

    private void prepararUsuario(UsuarioEntity usuario) {
        when(auth.getName()).thenReturn(EMAIL);
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(usuario));
    }

    private UsuarioEntity usuario() {
        return UsuarioEntity.builder()
                .id(UUID.randomUUID())
                .nome("Usuário")
                .email(EMAIL)
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build();
    }
}
