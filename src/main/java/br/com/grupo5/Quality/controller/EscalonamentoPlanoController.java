package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.RevisarPrazoEscalonamentoRequestDTO;
import br.com.grupo5.Quality.dto.response.EscalonamentoPainelResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.EscalonamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/escalonamentos")
@RequiredArgsConstructor
public class EscalonamentoPlanoController {

    private final EscalonamentoService escalonamentoService;

    @GetMapping
    public PaginaResponseDTO<EscalonamentoPainelResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PageableDefault(
                    size = 50,
                    sort = "escalonadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return escalonamentoService.listarDoSuperior(auth, planoId, pageable);
    }

    @PatchMapping("/{escalonamentoId}/prazo")
    public EscalonamentoPainelResponseDTO revisarPrazo(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID escalonamentoId,
            @Valid @RequestBody RevisarPrazoEscalonamentoRequestDTO dto
    ) {
        return escalonamentoService.revisarPrazo(
                auth,
                planoId,
                escalonamentoId,
                dto
        );
    }
}
