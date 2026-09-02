package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;

    public PlanoResponseDTO create(PlanoRequestDTO dto) {
        PlanoEntity entity = PlanoEntity.builder()
                .criador(dto.criador())
                .nomeProjeto(dto.nomeProjeto())
                .versao(dto.versao())
                .objetivo(dto.objetivo())
                .visaoGeral(dto.visaoGeral())
                .status(Status.valueOf(dto.status()))
                .criadoEm(LocalDateTime.now())
                .build();
        PlanoEntity saved = planoRepository.save(entity);
        return toDto(saved);
    }

    public Page<PlanoResponseDTO> findAll(Pageable pageable) {
        return planoRepository.findAll(pageable).map(this::toDto);
    }

    public PlanoResponseDTO findById(UUID id) throws NotFoundException {
        return planoRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
    }

    public PlanoResponseDTO update(UUID id, PlanoRequestDTO dto) throws NotFoundException {
        PlanoEntity entity = planoRepository.findById(id).orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        entity.setCriador(dto.criador());
        entity.setNomeProjeto(dto.nomeProjeto());
        entity.setVersao(dto.versao());
        entity.setObjetivo(dto.objetivo());
        entity.setVisaoGeral(dto.visaoGeral());
        entity.setStatus(Status.valueOf(dto.status()));
        PlanoEntity saved = planoRepository.save(entity);
        return toDto(saved);
    }

    public void delete(UUID id) {
        planoRepository.deleteById(id);
    }

    private PlanoResponseDTO toDto(PlanoEntity p) {
        return new PlanoResponseDTO(
                p.getId(),
                p.getCriador(),
                p.getNomeProjeto(),
                p.getVersao(),
                p.getObjetivo(),
                p.getVisaoGeral(),
                p.getStatus() != null ? p.getStatus().name() : null,
                p.getCriadoEm()
        );
    }
}
