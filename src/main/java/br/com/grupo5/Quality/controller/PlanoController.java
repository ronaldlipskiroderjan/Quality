package br.com.grupo5.Quality.controller;

import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import br.com.grupo5.Quality.service.PlanoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/planos")
@RequiredArgsConstructor
public class PlanoController {

    private final PlanoService planoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PlanoResponseDTO create(@RequestBody PlanoRequestDTO dto) {
        return planoService.create(dto);
    }

    @GetMapping
    public Page<PlanoResponseDTO> list(@PageableDefault(size = 15) Pageable pageable) {
        return planoService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public PlanoResponseDTO get(@PathVariable UUID id) throws NotFoundException {
        return planoService.findById(id);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public PlanoResponseDTO update(@PathVariable UUID id, @RequestBody PlanoRequestDTO dto) throws NotFoundException {
        return planoService.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable UUID id) {
        planoService.delete(id);
    }
}
