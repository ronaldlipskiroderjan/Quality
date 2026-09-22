package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.response.AuditoriaAgendaResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.ArtefatoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/minhas-auditorias")
@RequiredArgsConstructor
public class MinhaAuditoriaController {

    private final ArtefatoService artefatoService;

    @GetMapping
    public PaginaResponseDTO<AuditoriaAgendaResponseDTO> listar(
            Authentication auth,
            @PageableDefault(
                    size = 100,
                    sort = "dataPlanejada",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return artefatoService.listarAgenda(auth, pageable);
    }
}
