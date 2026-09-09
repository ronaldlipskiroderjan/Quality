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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TOKEN_TYPE = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    @Transactional
    public UsuarioResponseDTO registrar(UsuarioRequestDTO dto) {
        String email = normalizarEmail(dto.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new AlreadyExistsException("E-mail já cadastrado.");
        }

        RoleEntity roleUser = roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name())
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder().nome(RoleTypeEnum.ROLE_USER.name()).build()
                ));

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nome(dto.nome().trim())
                .email(email)
                .senhaHash(passwordEncoder.encode(dto.senha()))
                .roles(new HashSet<>(Set.of(roleUser)))
                .ativo(true)
                .criadoEm(LocalDateTime.now())
                .build();

        return toResponse(usuarioRepository.save(usuario));
    }

    public TokenResponseDTO autenticar(UsuarioLoginRequestDTO dto) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizarEmail(dto.email()),
                            dto.senha()
                    )
            );
            return criarToken(auth);
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new UnauthorizedException("E-mail ou senha inválidos.");
        }
    }

    public TokenResponseDTO renovar(Authentication auth) {
        return criarToken(auth);
    }

    private TokenResponseDTO criarToken(Authentication auth) {
        return new TokenResponseDTO(
                tokenProvider.gerarToken(auth),
                TOKEN_TYPE,
                tokenProvider.getExpirationTime()
        );
    }

    private UsuarioResponseDTO toResponse(UsuarioEntity usuario) {
        Set<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                roles
        );
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
