package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.response.NotificacaoResponseDTO;
import br.com.grupo5.Quality.service.NotificacaoService;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    @GetMapping
    public PaginaResponseDTO<NotificacaoResponseDTO> listar(
            Authentication auth,
            @PageableDefault(
                    size = 15,
                    sort = "criadaEm",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return notificacaoService.listar(auth, pageable);
    }

    @PostMapping("/{notificacaoId}/leitura")
    public NotificacaoResponseDTO marcarComoLida(
            Authentication auth,
            @PathVariable UUID notificacaoId
    ) {
        return notificacaoService.marcarComoLida(auth, notificacaoId);
    }
}
