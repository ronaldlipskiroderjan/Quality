package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.AtualizarClassificacoesPlanoRequestDTO;
import br.com.grupo5.Quality.dto.request.FeriadoPlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.ConfiguracaoClassificacaoResponseDTO;
import br.com.grupo5.Quality.dto.response.FeriadoPlanoResponseDTO;
import br.com.grupo5.Quality.service.CalendarioPrazoService;
import br.com.grupo5.Quality.service.ConfiguracaoPlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/configuracao")
@RequiredArgsConstructor
public class ConfiguracaoPlanoController {

    private final ConfiguracaoPlanoService configuracaoPlanoService;
    private final CalendarioPrazoService calendarioPrazoService;

    @GetMapping("/classificacoes")
    public List<ConfiguracaoClassificacaoResponseDTO> listarClassificacoes(
            Authentication auth,
            @PathVariable UUID planoId
    ) {
        return configuracaoPlanoService.listarClassificacoes(auth, planoId);
    }

    @PutMapping("/classificacoes")
    public List<ConfiguracaoClassificacaoResponseDTO> atualizarClassificacoes(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody AtualizarClassificacoesPlanoRequestDTO dto
    ) {
        return configuracaoPlanoService.atualizarClassificacoes(
                auth,
                planoId,
                dto
        );
    }

    @GetMapping("/feriados")
    public List<FeriadoPlanoResponseDTO> listarFeriados(
            Authentication auth,
            @PathVariable UUID planoId
    ) {
        return calendarioPrazoService.listar(auth, planoId);
    }

    @PostMapping("/feriados")
    @ResponseStatus(CREATED)
    public FeriadoPlanoResponseDTO adicionarFeriado(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @RequestBody FeriadoPlanoRequestDTO dto
    ) {
        return calendarioPrazoService.adicionar(auth, planoId, dto);
    }

    @DeleteMapping("/feriados/{feriadoId}")
    @ResponseStatus(NO_CONTENT)
    public void removerFeriado(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID feriadoId
    ) {
        calendarioPrazoService.remover(auth, planoId, feriadoId);
    }
}
