package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.CriarEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoResponseDTO;
import br.com.grupo5.Quality.service.CriacaoEscalonamentoResultado;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.EscalonamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(
        "/v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/escalonamentos"
)
@RequiredArgsConstructor
public class EscalonamentoController {

    private final EscalonamentoService escalonamentoService;

    @PostMapping
    public ResponseEntity<EscalonamentoResponseDTO> criar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @RequestHeader("Idempotency-Key") String chaveIdempotencia,
            @Valid @RequestBody CriarEscalonamentoRequestDTO dto
    ) {
        CriacaoEscalonamentoResultado resultado =
                escalonamentoService.criar(
                        auth,
                        planoId,
                        naoConformidadeId,
                        chaveIdempotencia,
                        dto
                );
        if (!resultado.criado()) {
            return ResponseEntity.ok(resultado.escalonamento());
        }

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resultado.escalonamento().id())
                .toUri();
        return ResponseEntity.created(local)
                .body(resultado.escalonamento());
    }

    @GetMapping
    public PaginaResponseDTO<EscalonamentoResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PageableDefault(
                    size = 15,
                    sort = "escalonadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return escalonamentoService.listar(
                auth,
                planoId,
                naoConformidadeId,
                pageable
        );
    }

    @GetMapping("/{escalonamentoId}")
    public EscalonamentoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PathVariable UUID escalonamentoId
    ) {
        return escalonamentoService.buscar(
                auth,
                planoId,
                naoConformidadeId,
                escalonamentoId
        );
    }
}
