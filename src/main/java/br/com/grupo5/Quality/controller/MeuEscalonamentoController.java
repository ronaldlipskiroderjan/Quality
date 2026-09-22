package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.response.EscalonamentoPainelResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.service.EscalonamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/meus-escalonamentos")
@RequiredArgsConstructor
public class MeuEscalonamentoController {

    private final EscalonamentoService escalonamentoService;

    @GetMapping
    public PaginaResponseDTO<EscalonamentoPainelResponseDTO> listar(
            Authentication auth,
            @PageableDefault(
                    size = 50,
                    sort = "escalonadoEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return escalonamentoService.listarAtribuidosAoSuperior(auth, pageable);
    }
}
