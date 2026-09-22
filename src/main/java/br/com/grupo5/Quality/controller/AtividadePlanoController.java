package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.response.AtividadePlanoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.AtividadePlanoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/atividades")
@RequiredArgsConstructor
public class AtividadePlanoController {

    private final AtividadePlanoService atividadePlanoService;

    @GetMapping
    public PaginaResponseDTO<AtividadePlanoResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PageableDefault(
                    size = 30,
                    sort = "criadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return atividadePlanoService.listar(auth, planoId, pageable);
    }
}
