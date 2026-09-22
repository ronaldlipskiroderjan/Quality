package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.InformarResolucaoRequestDTO;
import br.com.grupo5.Quality.dto.request.ValidarResolucaoRequestDTO;
import br.com.grupo5.Quality.dto.response.ResolucaoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.ResolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(
        "/v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/resolucoes"
)
@RequiredArgsConstructor
public class ResolucaoController {

    private final ResolucaoService resolucaoService;

    @PostMapping
    public ResponseEntity<ResolucaoResponseDTO> informar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @Valid @RequestBody InformarResolucaoRequestDTO dto
    ) {
        ResolucaoResponseDTO resolucao = resolucaoService.informar(
                auth,
                planoId,
                naoConformidadeId,
                dto
        );
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resolucao.id())
                .toUri();

        return ResponseEntity.created(local).body(resolucao);
    }

    @GetMapping
    public PaginaResponseDTO<ResolucaoResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PageableDefault(
                    size = 15,
                    sort = "informadaEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return resolucaoService.listar(
                auth,
                planoId,
                naoConformidadeId,
                pageable
        );
    }

    @GetMapping("/{resolucaoId}")
    public ResolucaoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PathVariable UUID resolucaoId
    ) {
        return resolucaoService.buscar(
                auth,
                planoId,
                naoConformidadeId,
                resolucaoId
        );
    }

    @PostMapping("/{resolucaoId}/validacao")
    public ResolucaoResponseDTO validar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PathVariable UUID resolucaoId,
            @Valid @RequestBody ValidarResolucaoRequestDTO dto
    ) {
        return resolucaoService.validar(
                auth,
                planoId,
                naoConformidadeId,
                resolucaoId,
                dto
        );
    }
}
