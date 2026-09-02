package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.PastOrPresentValidatorForCalendar;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PastOrPresentValidatorForCalendar pastOrPresentValidatorForCalendar;
    private final PasswordEncoder passwordEncoder;

    public void saveImagem(Authentication authentication, MultipartFile imagem) throws Exception{
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não econtrado..."));
        usuario.setFotoPerfil(imagem.getBytes());
        usuario.setContentType(imagem.getContentType());
        usuarioRepository.save(usuario);
    }

    public ImagemResponseDTO findImagem(Authentication authentication) throws Exception {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(u -> new ImagemResponseDTO(
                        u.getContentType(),
                        u.getFotoPerfil()
                ))
                .orElseThrow(() -> new NotFoundException("Usuário não econtrado..."));
    }

    public UsuarioResponseDTO findMe(Authentication authentication) throws Exception {
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(u -> new UsuarioResponseDTO(
                        u.getNome(),
                        u.getEmail()
                ))
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado..."));
    }

    public Page<UsuarioAdminResponseDTO> findAll(Pageable pageable) {
        return usuarioRepository.findAll(pageable)
                .map(u -> new UsuarioAdminResponseDTO(
                        u.getId(),
                        u.getNome(),
                        u.getEmail(),
                        u.isAtivo(),
                        u.getCriadoEm()
                ));
    }

    public void updatePassword(Authentication authentication, PasswordRequestDTO dto) throws Exception {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não econtrado..."));
        if (Objects.equals(usuario.getSenhaHash(), dto.senhaAntiga()) && Objects.equals(dto.novaSenha(), dto.confirmacaoSenha())) {
            usuario.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        } else {
            throw new BadRequestException("As Senhas não correspondem...");
        }
    }

    public void updateMe(Authentication authentication, UsuarioUpdateRequestDTO dto) throws Exception {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não econtrado..."));
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuarioRepository.save(usuario);
    }
}
