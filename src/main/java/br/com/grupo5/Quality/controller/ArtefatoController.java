package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.ArtefatoRequestDTO;
import br.com.grupo5.Quality.dto.response.ArtefatoResponseDTO;
import br.com.grupo5.Quality.service.ArtefatoService;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/artefatos")
@RequiredArgsConstructor
public class ArtefatoController {

    private final ArtefatoService artefatoService;

    @PostMapping
    public ResponseEntity<ArtefatoResponseDTO> criar(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody ArtefatoRequestDTO dto
    ) {
        ArtefatoResponseDTO artefato = artefatoService.criar(
                auth,
                planoId,
                dto
        );
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(artefato.id())
                .toUri();

        return ResponseEntity.created(local).body(artefato);
    }

    @GetMapping
    public PaginaResponseDTO<ArtefatoResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PageableDefault(size = 15, sort = "dataPlanejada") Pageable pageable
    ) {
        return artefatoService.listar(auth, planoId, pageable);
    }

    @GetMapping("/{artefatoId}")
    public ArtefatoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId
    ) {
        return artefatoService.buscar(auth, planoId, artefatoId);
    }

    @PutMapping("/{artefatoId}")
    public ArtefatoResponseDTO atualizar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId,
            @Valid @RequestBody ArtefatoRequestDTO dto
    ) {
        return artefatoService.atualizar(auth, planoId, artefatoId, dto);
    }

    @DeleteMapping("/{artefatoId}")
    public ResponseEntity<Void> remover(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID artefatoId
    ) {
        artefatoService.remover(auth, planoId, artefatoId);
        return ResponseEntity.noContent().build();
    }
}
