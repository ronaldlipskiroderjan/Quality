package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.EncaminhamentoResultado;
import br.com.grupo5.Quality.dto.response.EncaminhamentoResponseDTO;
import br.com.grupo5.Quality.service.EncaminhamentoNcService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(
        "/v1/planos/{planoId}/nao-conformidades/{naoConformidadeId}/encaminhamentos"
)
@RequiredArgsConstructor
public class EncaminhamentoNcController {

    private final EncaminhamentoNcService encaminhamentoService;

    @PostMapping
    public ResponseEntity<EncaminhamentoResponseDTO> encaminhar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID naoConformidadeId,
            @RequestHeader("Idempotency-Key") String chaveIdempotencia
    ) {
        EncaminhamentoResultado resultado =
                encaminhamentoService.encaminhar(
                        auth,
                        planoId,
                        naoConformidadeId,
                        chaveIdempotencia
                );
        if (!resultado.criado()) {
            return ResponseEntity.ok(resultado.encaminhamento());
        }

        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resultado.encaminhamento().id())
                .toUri();
        return ResponseEntity.created(local)
                .body(resultado.encaminhamento());
    }
}
