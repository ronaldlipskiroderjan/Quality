package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.repository.PlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcessoPlanoService {

    private final PlanoRepository planoRepository;

    @Transactional(readOnly = true)
    public PlanoEntity buscar(Authentication auth, UUID planoId) {
        return planoRepository.findByIdAndUsuarioEmail(planoId, auth.getName())
                .orElseThrow(() -> new NotFoundException("Plano não encontrado."));
    }
}
