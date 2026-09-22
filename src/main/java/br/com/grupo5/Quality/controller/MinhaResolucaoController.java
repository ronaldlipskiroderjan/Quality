package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.response.NaoConformidadeResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.NaoConformidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/minhas-nao-conformidades")
@RequiredArgsConstructor
public class MinhaResolucaoController {

    private final NaoConformidadeService naoConformidadeService;

    @GetMapping
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listar(
            Authentication auth,
            @PageableDefault(
                    size = 15,
                    sort = "atualizadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return naoConformidadeService.listarDaEquipe(auth, pageable);
    }

    @GetMapping("/atribuidas")
    public PaginaResponseDTO<NaoConformidadeResponseDTO> listarAtribuidas(
            Authentication auth,
            @PageableDefault(
                    size = 50,
                    sort = "atualizadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return naoConformidadeService.listarAtribuidas(auth, pageable);
    }
}
