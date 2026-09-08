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
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.NotFoundException;
import org.apache.coyote.BadRequestException;
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
    void deveRegistrarUsuarioComSenhaCodificadaRoleUserEValoresIniciais() throws Exception {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Usuario", EMAIL, "senha123");
        RoleEntity roleUser = RoleEntity.builder().nome(RoleTypeEnum.ROLE_USER.name()).build();
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name())).thenReturn(Optional.of(roleUser));
        when(passwordEncoder.encode("senha123")).thenReturn("senha-codificada");

        service.register(dto);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        UsuarioEntity salvo = captor.getValue();
        assertEquals("Usuario", salvo.getNome());
        assertEquals(EMAIL, salvo.getEmail());
        assertEquals("senha-codificada", salvo.getSenhaHash());
        assertTrue(salvo.getRoles().contains(roleUser));
        assertTrue(salvo.isAtivo());
        assertNotNull(salvo.getCriadoEm());
    }

    @Test
    void deveCriarRoleUserQuandoElaAindaNaoExiste() throws Exception {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Usuario", EMAIL, "senha123");
        RoleEntity roleCriada = RoleEntity.builder().nome(RoleTypeEnum.ROLE_USER.name()).build();
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name())).thenReturn(Optional.empty());
        when(roleRepository.save(any(RoleEntity.class))).thenReturn(roleCriada);

        service.register(dto);

        ArgumentCaptor<RoleEntity> captor = ArgumentCaptor.forClass(RoleEntity.class);
        verify(roleRepository).save(captor.capture());
        assertEquals(RoleTypeEnum.ROLE_USER.name(), captor.getValue().getNome());
    }

    @Test
    void naoDeveRegistrarEmailJaExistente() {
        AuthService service = service();
        UsuarioRequestDTO dto = new UsuarioRequestDTO("Usuario", EMAIL, "senha123");
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThrows(AlreadyExistsException.class, () -> service.register(dto));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveAutenticarEGerarToken() throws Exception {
        AuthService service = service();
        UsuarioLoginRequestDTO dto = new UsuarioLoginRequestDTO(EMAIL, "senha123");
        Authentication authentication = new UsernamePasswordAuthenticationToken(EMAIL, "senha123");
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.gerarToken(authentication)).thenReturn("jwt");
        when(tokenProvider.getExpirationTime()).thenReturn(900_000L);

        TokenResponseDTO response = service.login(dto);

        assertEquals("jwt", response.token());
        assertEquals(900_000L, response.expiration());
    }

    @Test
    void deveRecusarLoginDeUsuarioInexistente() {
        AuthService service = service();
        UsuarioLoginRequestDTO dto = new UsuarioLoginRequestDTO(EMAIL, "senha123");
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> service.login(dto));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void deveConverterCredenciaisInvalidasEmBadRequest() {
        AuthService service = service();
        UsuarioLoginRequestDTO dto = new UsuarioLoginRequestDTO(EMAIL, "senha-incorreta");
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("credenciais invalidas"));

        assertThrows(BadRequestException.class, () -> service.login(dto));
    }

    @Test
    void deveAtualizarTokenDoUsuarioAutenticado() {
        AuthService service = service();
        Authentication authentication = new UsernamePasswordAuthenticationToken(EMAIL, null);
        when(tokenProvider.gerarToken(authentication)).thenReturn("jwt-atualizado");
        when(tokenProvider.getExpirationTime()).thenReturn(900_000L);

        TokenResponseDTO response = service.refreshToken(authentication);

        assertEquals("jwt-atualizado", response.token());
        assertEquals(900_000L, response.expiration());
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
