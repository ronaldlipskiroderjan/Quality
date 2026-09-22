package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.ImagemResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanoDetalhadoResponseDTO> criar(
            Authentication auth,
            @Valid @RequestBody PlanoRequestDTO dto
    ) {
        PlanoDetalhadoResponseDTO plano = planoService.criar(auth, dto);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(plano.id())
                .toUri();

        return ResponseEntity.created(local).body(plano);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanoDetalhadoResponseDTO> criarComImagem(
            Authentication auth,
            @Valid @RequestPart("plano") PlanoRequestDTO dto,
            @RequestPart("imagem") MultipartFile imagem
    ) {
        PlanoDetalhadoResponseDTO plano = planoService.criar(auth, dto, imagem);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(plano.id())
                .toUri();

        return ResponseEntity.created(local).body(plano);
    }

    @GetMapping
    public PaginaResponseDTO<PlanoResponseDTO> listar(
            Authentication auth,
            @PageableDefault(
                    size = 15,
                    sort = "plano.criadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return planoService.listar(auth, pageable);
    }

    @GetMapping("/{id}")
    public PlanoDetalhadoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID id
    ) {
        return planoService.buscar(auth, id);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PlanoDetalhadoResponseDTO atualizar(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody PlanoRequestDTO dto
    ) {
        return planoService.atualizar(auth, id, dto);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlanoDetalhadoResponseDTO atualizarComImagem(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestPart("plano") PlanoRequestDTO dto,
            @RequestPart(value = "imagem", required = false) MultipartFile imagem,
            @RequestParam(defaultValue = "false") boolean removerImagem
    ) {
        return planoService.atualizar(auth, id, dto, imagem, removerImagem);
    }

    @PutMapping(value = "/{id}/imagem", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> salvarImagem(
            Authentication auth,
            @PathVariable UUID id,
            @RequestPart("imagem") MultipartFile imagem
    ) {
        planoService.salvarImagem(auth, id, imagem);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/imagem")
    public ResponseEntity<byte[]> buscarImagem(
            Authentication auth,
            @PathVariable UUID id
    ) {
        ImagemResponseDTO imagem = planoService.buscarImagem(auth, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.contentType()))
                .contentLength(imagem.imagem().length)
                .body(imagem.imagem());
    }

    @DeleteMapping("/{id}/imagem")
    public ResponseEntity<Void> removerImagem(
            Authentication auth,
            @PathVariable UUID id
    ) {
        planoService.removerImagem(auth, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/conclusao")
    public ResponseEntity<Void> concluir(
            Authentication auth,
            @PathVariable UUID id
    ) {
        planoService.concluir(auth, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            Authentication auth,
            @PathVariable UUID id
    ) {
        planoService.excluir(auth, id);
        return ResponseEntity.noContent().build();
    }
}
