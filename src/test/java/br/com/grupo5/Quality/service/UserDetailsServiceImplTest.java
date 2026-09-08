package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    private static final String EMAIL = "usuario@quality.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void deveCarregarUsuarioPeloEmail() {
        UsuarioEntity usuario = UsuarioEntity.builder().email(EMAIL).build();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(usuarioRepository);

        assertSame(usuario, service.loadUserByUsername(EMAIL));
    }

    @Test
    void deveInformarQuandoUsuarioNaoExiste() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(usuarioRepository);

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername(EMAIL));
    }
}
