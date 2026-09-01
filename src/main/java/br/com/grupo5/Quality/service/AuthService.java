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
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenProvider tokenProvider;

    public void register(UsuarioRequestDTO dto) throws Exception{
        if (usuarioRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new AlreadyExistsException("Usuário já cadastrado...");
        }
        RoleEntity roleEntity = roleRepository.findByNome(RoleTypeEnum.ROLE_USER.name())
                .orElseGet(()-> roleRepository.save(RoleEntity.builder()
                        .nome(RoleTypeEnum.ROLE_USER.name())
                        .build()));
        usuarioRepository.save(UsuarioEntity.builder()
                        .nome(dto.nome())
                        .email(dto.email())
                        .senhaHash(passwordEncoder.encode(dto.senha()))
                .build());
    }

    public TokenResponseDTO login(UsuarioLoginRequestDTO dto) throws Exception {
        if (!usuarioRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new NotFoundException("Usuario não encontrado...");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.email(), dto.senha()));
            String token = tokenProvider.gerarToken(authentication);
            return new TokenResponseDTO(token, tokenProvider.getExpirationTime());
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Informações inválidas...");
        } catch (Exception ex) {
            throw  ex;
        }
    }

    public TokenResponseDTO refreshToken(Authentication authentication) {
        String token = tokenProvider.gerarToken(authentication);
        return new TokenResponseDTO(token, tokenProvider.getExpirationTime());
    }
}