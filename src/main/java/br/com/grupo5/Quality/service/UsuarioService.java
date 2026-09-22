package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final long TAMANHO_MAXIMO_IMAGEM = 5 * 1024 * 1024;
    private static final Set<String> TIPOS_IMAGEM_PERMITIDOS = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void salvarImagem(Authentication auth, MultipartFile imagem) {
        validarImagem(imagem);
        UsuarioEntity usuario = buscarUsuario(auth);
        usuario.setFotoPerfil(lerImagem(imagem));
        usuario.setTipoImagem(imagem.getContentType());
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public ImagemResponseDTO buscarImagem(Authentication auth) {
        UsuarioEntity usuario = buscarUsuario(auth);
        if (usuario.getFotoPerfil() == null) {
            throw new NotFoundException("Imagem de perfil não encontrada.");
        }
        return new ImagemResponseDTO(usuario.getTipoImagem(), usuario.getFotoPerfil());
    }

    @Transactional
    public void removerImagem(Authentication auth) {
        UsuarioEntity usuario = buscarUsuario(auth);
        usuario.setFotoPerfil(null);
        usuario.setTipoImagem(null);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPerfil(Authentication auth) {
        return toResponse(buscarUsuario(auth));
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<UsuarioAdminResponseDTO> listar(Pageable pageable) {
        return PaginaResponseDTO.de(usuarioRepository.findAll(pageable)
                .map(usuario -> new UsuarioAdminResponseDTO(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.isAtivo(),
                        usuario.getCriadoEm()
                )));
    }

    @Transactional
    public void alterarSenha(Authentication auth, PasswordRequestDTO dto) {
        UsuarioEntity usuario = buscarUsuario(auth);

        if (!dto.novaSenha().equals(dto.confirmacaoSenha())) {
            throw new InvalidRequestException("A confirmação da senha não corresponde.");
        }
        if (!passwordEncoder.matches(dto.senhaAntiga(), usuario.getSenhaHash())) {
            throw new InvalidRequestException("A senha atual está incorreta.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioResponseDTO atualizarPerfil(
            Authentication auth,
            UsuarioUpdateRequestDTO dto
    ) {
        UsuarioEntity usuario = buscarUsuario(auth);
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        if (!usuario.getEmail().equalsIgnoreCase(email)
                && usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new AlreadyExistsException("E-mail já cadastrado.");
        }

        usuario.setNome(dto.nome().trim());
        usuario.setEmail(email);
        return toResponse(usuarioRepository.save(usuario));
    }

    private UsuarioEntity buscarUsuario(Authentication auth) {
        return usuarioRepository.findByEmailIgnoreCase(auth.getName())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado."));
    }

    private void validarImagem(MultipartFile imagem) {
        String tipo = imagem.getContentType();
        if (imagem.isEmpty() || tipo == null
                || !TIPOS_IMAGEM_PERMITIDOS.contains(tipo.toLowerCase(Locale.ROOT))) {
            throw new InvalidRequestException(
                    "Envie uma imagem JPG, PNG ou WebP."
            );
        }
        if (imagem.getSize() > TAMANHO_MAXIMO_IMAGEM) {
            throw new InvalidRequestException(
                    "A imagem de perfil deve possuir no máximo 5 MB."
            );
        }
    }

    private byte[] lerImagem(MultipartFile imagem) {
        try {
            return imagem.getBytes();
        } catch (IOException ex) {
            throw new InvalidRequestException("Não foi possível ler a imagem.");
        }
    }

    private UsuarioResponseDTO toResponse(UsuarioEntity usuario) {
        Set<String> roles = usuario.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toUnmodifiableSet());

        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getFotoPerfil() != null,
                roles
        );
    }
}
