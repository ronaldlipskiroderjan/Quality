package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PasswordRequestDTO;
import br.com.grupo5.Quality.dto.request.UsuarioUpdateRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioAdminResponseDTO;
import br.com.grupo5.Quality.dto.response.UsuarioResponseDTO;
import br.com.grupo5.Quality.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/me/imagem")
    @ResponseStatus(HttpStatus.CREATED)
    public void addPicture(Authentication authentication, @RequestParam MultipartFile imagem) throws Exception{
        usuarioService.saveImagem(authentication, imagem);
    }

    @GetMapping("/v1/imagem")
    public ResponseEntity<byte[]> findImagem(Authentication authentication) throws Exception {
        ImagemResponseDTO dto = usuarioService.findImagem(authentication);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(dto.contentType())).body(dto.imagem());
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public UsuarioResponseDTO findMe(Authentication authentication) throws Exception {
        return usuarioService.findMe(authentication);
    }

    @GetMapping("/admin/all")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UsuarioAdminResponseDTO> findAll(@PageableDefault(size = 15) Pageable pageable) throws Exception{
        return usuarioService.findAll(pageable);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updatePassword(Authentication authentication, PasswordRequestDTO dto) throws Exception {
        usuarioService.updatePassword(authentication, dto);
    };

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void updateMe(Authentication authentication, UsuarioUpdateRequestDTO dto) throws Exception {
        usuarioService.updateMe(authentication, dto);
    };
}
