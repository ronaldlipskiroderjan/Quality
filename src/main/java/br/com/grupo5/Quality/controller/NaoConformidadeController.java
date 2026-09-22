package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.AtualizarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.request.CriarNaoConformidadeRequestDTO;
import br.com.grupo5.Quality.dto.response.EventoNaoConformidadeResponseDTO;
import br.com.grupo5.Quality.dto.response.NaoConformidadeResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.HistoricoNaoConformidadeService;
import br.com.grupo5.Quality.service.AlertaEquipeResolucaoService;
import br.com.grupo5.Quality.service.NaoConformidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/nao-conformidades")
@RequiredArgsConstructor
public class NaoConformidadeController {

    private final NaoConformidadeService naoConformidadeService;
    private final HistoricoNaoConformidadeService historicoService;
    private final AlertaEquipeResolucaoService alertaEquipeService;

    @PostMapping
    public ResponseEntity<NaoConformidadeResponseDTO> criar(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody CriarNaoConformidadeRequestDTO dto
    ) {
        NaoConformidadeResponseDTO naoConformidade =
                naoConformidadeService.criar(auth, planoId, dto);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(naoConformidade.id())
                .toUri();

        return ResponseEntity.created(local).body(naoConformidade);
    }

    @PostMapping("/rascunhos")
    public ResponseEntity<NaoConformidadeResponseDTO> criarRascunho(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody CriarNaoConformidadeRequestDTO dto
    ) {
        NaoConformidadeResponseDTO naoConformidade =
                naoConformidadeService.criarRascunho(auth, planoId, dto);
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(naoConformidade.id())
                .toUri();
        return ResponseEntity.created(local).body(naoConformidade);
    }

    @GetMapping
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PageableDefault(
                    size = 15,
                    sort = "identificadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return naoConformidadeService.listar(auth, planoId, pageable);
    }

    @GetMapping("/equipe")
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listarDaEquipe(
            Authentication auth,
            @PathVariable UUID planoId,
            @PageableDefault(
                    size = 50,
                    sort = "atualizadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return naoConformidadeService.listarDaEquipeNoPlano(
                auth,
                planoId,
                pageable
        );
    }

    @GetMapping("/{naoConformidadeId}")
    public NaoConformidadeResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId
    ) {
        return naoConformidadeService.buscar(
                auth,
                planoId,
                naoConformidadeId
        );
    }

    @PutMapping("/{naoConformidadeId}")
    public NaoConformidadeResponseDTO atualizar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @Valid @RequestBody AtualizarNaoConformidadeRequestDTO dto
    ) {
        return naoConformidadeService.atualizar(
                auth,
                planoId,
                naoConformidadeId,
                dto
        );
    }

    @GetMapping("/{naoConformidadeId}/historico")
    public PaginaResponseDTO<EventoNaoConformidadeResponseDTO> listarHistorico(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @PageableDefault(
                    size = 15,
                    sort = "ocorridoEm",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return historicoService.listar(
                auth,
                planoId,
                naoConformidadeId,
                pageable
        );
    }

    @PostMapping("/{naoConformidadeId}/alertas-equipe")
    public ResponseEntity<Void> alertarEquipe(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId
    ) {
        alertaEquipeService.emitir(auth, planoId, naoConformidadeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{naoConformidadeId}")
    public ResponseEntity<Void> remover(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId
    ) {
        naoConformidadeService.remover(
                auth,
                planoId,
                naoConformidadeId
        );
        return ResponseEntity.noContent().build();
    }
}
