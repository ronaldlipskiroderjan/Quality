package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.database.enums.Classificacao;
import br.com.grupo5.Quality.dto.request.DocumentoRequestDTO;
import br.com.grupo5.Quality.dto.response.DocumentoArquivoResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.DocumentoResponseDTO;
import br.com.grupo5.Quality.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos/{planoId}/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoDetalhadoResponseDTO> adicionar(
            Authentication auth,
            @PathVariable UUID planoId,
            @Valid @ModelAttribute DocumentoRequestDTO dto
    ) {
        DocumentoDetalhadoResponseDTO documento = documentoService.adicionar(
                auth,
                planoId,
                dto
        );
        URI local = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(documento.id())
                .toUri();

        return ResponseEntity.created(local).body(documento);
    }

    @GetMapping
    public List<DocumentoResponseDTO> listar(
            Authentication auth,
            @PathVariable UUID planoId,
            @RequestParam(required = false) Classificacao classificacao
    ) {
        return documentoService.listar(auth, planoId, classificacao);
    }

    @GetMapping("/{documentoId}")
    public DocumentoDetalhadoResponseDTO buscar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID documentoId
    ) {
        return documentoService.buscar(auth, planoId, documentoId);
    }

    @GetMapping("/{documentoId}/arquivo")
    public ResponseEntity<byte[]> baixar(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID documentoId
    ) {
        DocumentoArquivoResponseDTO arquivo = documentoService.baixar(
                auth,
                planoId,
                documentoId
        );
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.tipo()))
                .contentLength(arquivo.conteudo().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(arquivo.conteudo());
    }

    @DeleteMapping("/{documentoId}")
    public ResponseEntity<Void> remover(
            Authentication auth,
            @PathVariable UUID planoId,
            @PathVariable UUID documentoId
    ) {
        documentoService.remover(auth, planoId, documentoId);
        return ResponseEntity.noContent().build();
    }
}
