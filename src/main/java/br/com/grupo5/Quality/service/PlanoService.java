package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.UsuarioEntity;
import br.com.grupo5.Quality.database.enums.Status;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.database.repository.UsuarioRepository;
import br.com.grupo5.Quality.dto.request.PlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.PlanoDetalhadoResponseDTO;
import br.com.grupo5.Quality.dto.response.PlanoResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final UsuarioRepository usuarioRepository;

    public void create(Authentication authentication, PlanoRequestDTO dto) throws Exception {
        UsuarioEntity usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        PlanoEntity newPlano = planoRepository.save(PlanoEntity.builder()
                        .nomeProjeto(dto.nomeProjeto())
                        .versao(dto.versao())
                        .objetivo(dto.objetivo())
                        .visaoGeral(dto.visaoGeral())
                        .status(Status.PENDENTE)
                        .criadoEm(LocalDateTime.now())
                .build());
        usuario.getPlanos().add(newPlano);
        usuarioRepository.save(usuario);
    }

    public List<PlanoResponseDTO> findAll(UUID id) throws Exception {
        UsuarioEntity usuario = usuarioRepository.findByIdWithPlanos(id)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        return usuario.getPlanos().stream()
                .map(this::toDto)
                .toList();
    }

    public PlanoResponseDTO findById(UUID id) throws NotFoundException {
        return planoRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
    }

    public void update(UUID id, PlanoRequestDTO dto) throws NotFoundException {
        PlanoEntity plano = planoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plano.setNomeProjeto(dto.nomeProjeto());
        plano.setVersao(dto.versao());
        plano.setObjetivo(dto.objetivo());
        plano.setVisaoGeral(dto.visaoGeral());
        planoRepository.save(plano);
    }

    public void closePlano(UUID id) throws Exception{
        PlanoEntity plano = planoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Plano não encontrado"));
        plano.setStatus(Status.CONCLUIDO);
        planoRepository.save(plano);
    }

    public void delete(UUID id) throws Exception {
        if (!planoRepository.existsById(id)) {
            throw new NotFoundException("Plano não encontrado");
        }
        planoRepository.deleteById(id);
    }

    private PlanoResponseDTO toDto(PlanoEntity p) {
        return new PlanoResponseDTO(
                p.getId(),
                p.getNomeProjeto(),
                p.getStatus()
        );
    }
}
