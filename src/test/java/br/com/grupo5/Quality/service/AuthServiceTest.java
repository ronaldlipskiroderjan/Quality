package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.config.TokenProvider;
import br.com.grupo5.Quality.database.RoleEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.RoleTypeEnum;
import br.com.grupo5.Quality.database.repository.RoleRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.UsuarioLoginRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioRequestDTO;
import br.com.grupo5.Quality.dto.response.TokenResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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
class AuthServiceTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenProvider tokenProvider;

    @Test
    void deveRegistrarUsuarioAtivoComRoleUser() {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO(" Usuário ", " USUARIO@QUALITY.COM ", "senha123");
        RoleEntity role = RoleEntity.builder().nome(RoleTypeEnum.ROLE_USER.name()).build();

        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name()))
                .thenReturn(Optional.of(role));
        when(passwordEncoder.encode("senha123")).thenReturn("senha-codificada");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(invocation -> {
            UsuarioEntity usuario = invocation.getArgument(0);
            usuario.setId(UUID.randomUUID());
            return usuario;
        });

        UsuarioResponseDTO response = service.registrar(dto);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity usuario = captor.getValue();

        assertEquals("Usuário", usuario.getNome());
        assertEquals(EMAIL, usuario.getEmail());
        assertEquals("senha-codificada", usuario.getSenhaHash());
        assertTrue(usuario.isAtivo());
        assertTrue(usuario.getRoles().contains(role));
        assertNotNull(usuario.getCriadoEm());
        assertEquals(EMAIL, response.email());
        assertTrue(response.roles().contains(RoleTypeEnum.ROLE_USER.name()));
    }

    @Test
    void deveCriarRoleUserQuandoNecessario() {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Usuário", EMAIL, "senha123");
        RoleEntity role = RoleEntity.builder().nome(RoleTypeEnum.ROLE_USER.name()).build();

        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name())).thenReturn(Optional.empty());
        when(roleRepository.save(any(RoleEntity.class))).thenReturn(role);
        when(usuarioRepository.save(any(UsuarioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.registrar(dto);

        verify(roleRepository).save(any(RoleEntity.class));
    }

    @Test
    void deveRecusarEmailJaCadastrado() {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Usuário", EMAIL, "senha123");
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> service.registrar(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveAutenticarEGerarToken() {
        AuthService service = service();
        UsuarioLoginRequestDTO dto = new UsuarioLoginRequestDTO(EMAIL, "senha123");
        Authentication auth = new UsernamePasswordAuthenticationToken(EMAIL, null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenProvider.gerarToken(auth)).thenReturn("jwt");
        when(tokenProvider.getExpirationTime()).thenReturn(900_000L);

        TokenResponseDTO response = service.autenticar(dto);

        assertEquals("jwt", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(900_000L, response.expiresInMs());
    }

    @Test
    void deveRetornarErroGenericoParaCredenciaisInvalidas() {
        AuthService service = service();
        UsuarioLoginRequestDTO dto = new UsuarioLoginRequestDTO(EMAIL, "senha-incorreta");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("credenciais inválidas"));

        UnauthorizedException erro = assertThrows(
                UnauthorizedException.class,
                () -> service.autenticar(dto)
        );

        assertEquals("E-mail ou senha inválidos.", erro.getMessage());
    }

    @Test
    void deveRenovarToken() {
        AuthService service = service();
        Authentication auth = new UsernamePasswordAuthenticationToken(EMAIL, null);

        when(tokenProvider.gerarToken(auth)).thenReturn("jwt-atualizado");
        when(tokenProvider.getExpirationTime()).thenReturn(900_000L);

        TokenResponseDTO response = service.renovar(auth);

        assertEquals("jwt-atualizado", response.accessToken());
        assertEquals(900_000L, response.expiresInMs());
    }

    private AuthService service() {
        return new AuthService(
                usuarioRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                tokenProvider
        );
    }
}
