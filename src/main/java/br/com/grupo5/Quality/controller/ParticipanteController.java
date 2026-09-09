package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.NovoParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.request.PapeisParticipanteRequestDTO;
import br.com.grupo5.Quality.dto.response.ParticipanteResponseDTO;
import br.com.grupo5.Quality.service.ParticipanteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/participantes")
@RequiredArgsConstructor
public class ParticipanteController {

    private final ParticipanteService participanteService;

    @PostMapping
    public ResponseEntity<ParticipanteResponseDTO> adicionar(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody NovoParticipanteRequestDTO dto
    ) {
        ParticipanteResponseDTO participante = participanteService.adicionar(
                auth,
                planoId,
                dto
        );
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(participante.id())
                .toUri();

        return ResponseEntity.created(local).body(participante);
    }

    @GetMapping
    public List<ParticipanteResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId
    ) {
        return participanteService.listar(auth, planoId);
    }

    @PutMapping("/{participanteId}")
    public ParticipanteResponseDTO atualizar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID participanteId,
            @Valid @RequestBody PapeisParticipanteRequestDTO dto
    ) {
        return participanteService.atualizar(
                auth,
                planoId,
                participanteId,
                dto
        );
    }

    @DeleteMapping("/{participanteId}")
    public ResponseEntity<Void> remover(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID participanteId
    ) {
        participanteService.remover(auth, planoId, participanteId);
        return ResponseEntity.noContent().build();
    }
}
