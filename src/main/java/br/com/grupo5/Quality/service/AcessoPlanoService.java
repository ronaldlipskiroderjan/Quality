package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcessoPlanoService {

    private final ParticipacaoPlanoRepository participacaoRepository;

    @Transactional(readOnly = true)
    public ParticipacaoPlanoEntity buscarParticipacao(
            Authentication auth,
            UUID planoId
    ) {
        return participacaoRepository
                .findByPlanoIdAndUsuarioEmailIgnoreCase(planoId, auth.getName())
                .orElseThrow(() -> new NotFoundException("Plano não encontrado."));
    }

    @Transactional(readOnly = true)
    public ParticipacaoPlanoEntity buscarParticipacao(
            Authentication auth,
            UUID planoId,
            PermissaoPlano permissao
    ) {
        ParticipacaoPlanoEntity participacao = buscarParticipacao(auth, planoId);
        if (!participacao.possuiPermissao(permissao)) {
            throw new AccessDeniedException(
                    "Você não possui permissão para esta operação."
            );
        }
        return participacao;
    }

    @Transactional(readOnly = true)
    public PlanoEntity buscarPlano(Authentication auth, UUID planoId) {
        return buscarParticipacao(auth, planoId).getPlano();
    }

    @Transactional(readOnly = true)
    public PlanoEntity buscarPlano(
            Authentication auth,
            UUID planoId,
            PermissaoPlano permissao
    ) {
        return buscarParticipacao(auth, planoId, permissao).getPlano();
    }
}
