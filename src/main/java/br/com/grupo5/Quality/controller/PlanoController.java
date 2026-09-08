package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import br.com.grupo5.Quality.service.PlanoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(Authentication authentication,
                       @Valid @RequestBody PlanoRequestDTO dto) throws Exception{
        planoService.create(authentication, dto);
    }

    @GetMapping("/id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public List<PlanoResponseDTO> listAllMe(@PathVariable UUID id) throws Exception {
        return planoService.findAll(id);
    }

    @GetMapping("/{id}")
    public PlanoResponseDTO get(@PathVariable UUID id) throws Exception {
        return planoService.findById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void update(@PathVariable UUID id,
                       @Valid@RequestBody PlanoRequestDTO dto) throws Exception {
        planoService.update(id, dto);
    }

    @PatchMapping("/id/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void close(@PathVariable UUID id) throws Exception{
        planoService.closePlano(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) throws Exception {
        planoService.delete(id);
    }
}
