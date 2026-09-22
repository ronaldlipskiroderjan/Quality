package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return buscarUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado..."));
    }

    private Optional<UsuarioEntity> buscarUsuario(
            String identificador
    ) {
        try {
            return usuarioRepository.findById(UUID.fromString(identificador));
        } catch (IllegalArgumentException ex) {
            return usuarioRepository.findByEmailIgnoreCase(identificador);
        }
    }
}
