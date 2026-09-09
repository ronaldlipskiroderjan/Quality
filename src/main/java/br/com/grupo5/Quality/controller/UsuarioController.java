package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PutMapping(value = "/me/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> salvarImagem(
            Authentication auth,
            @RequestPart("imagem") MultipartFile imagem
    ) {
        usuarioService.salvarImagem(auth, imagem);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/imagem")
    public ResponseEntity<byte[]> buscarImagem(Authentication auth) {
        ImagemResponseDTO imagem = usuarioService.buscarImagem(auth);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .contentLength(imagem.imagem().length)
                .body(imagem.imagem());
    }

    @GetMapping("/me")
    public UsuarioResponseDTO buscarPerfil(Authentication auth) {
        return usuarioService.buscarPerfil(auth);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UsuarioAdminResponseDTO> listar(
            @PageableDefault(size = 15, sort = "nome") Pageable pageable
    ) {
        return usuarioService.listar(pageable);
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(
            Authentication auth,
            @Valid @RequestBody PasswordRequestDTO dto
    ) {
        usuarioService.alterarSenha(auth, dto);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me")
    public UsuarioResponseDTO atualizarPerfil(
            Authentication auth,
            @Valid @RequestBody UsuarioUpdateRequestDTO dto
    ) {
        return usuarioService.atualizarPerfil(auth, dto);
    }
}
