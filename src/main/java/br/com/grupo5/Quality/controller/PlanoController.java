package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @PostMapping
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

    @GetMapping
    public List<PlanoResponseDTO> listar(Authentication auth) {
        return planoService.listar(auth);
    }

    @GetMapping("/{id}")
    public PlanoDetalhadoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID id
    ) {
        return planoService.buscar(auth, id);
    }

    @PutMapping("/{id}")
    public PlanoDetalhadoResponseDTO atualizar(
            Authentication auth,
            @PathVariable UUID id,
            @Valid @RequestBody PlanoRequestDTO dto
    ) {
        return planoService.atualizar(auth, id, dto);
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
