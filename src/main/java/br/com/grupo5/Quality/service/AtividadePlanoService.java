package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.AtividadePlanoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.AtividadePlanoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.dto.response.AtividadePlanoResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AtividadePlanoService {

    private final AtividadePlanoRepository atividadeRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(
            UUID planoId,
            String email,
            String metodo,
            String caminho
    ) {
        ParticipacaoPlanoEntity autor = participacaoRepository
                .findByPlanoIdAndUsuarioEmailIgnoreCase(planoId, email)
                .orElse(null);
        if (autor == null
                || !autor.possuiPapel(PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE)) {
            return;
        }

        atividadeRepository.save(AtividadePlanoEntity.builder()
                .plano(autor.getPlano())
                .autor(autor)
                .acao(acao(metodo))
                .descricao(descricao(metodo, caminho))
                .criadoEm(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<AtividadePlanoResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            Pageable pageable
    ) {
        acessoPlanoService.buscarPlano(auth, planoId, PermissaoPlano.VISUALIZAR);
        return PaginaResponseDTO.de(atividadeRepository
                .findAllByPlanoId(planoId, pageable)
                .map(this::toResponse));
    }

    private String acao(String metodo) {
        return switch (metodo) {
            case "POST" -> "CRIACAO";
            case "PUT", "PATCH" -> "ALTERACAO";
            case "DELETE" -> "EXCLUSAO";
            default -> "ATUALIZACAO";
        };
    }

    private String descricao(String metodo, String caminho) {
        String recurso = caminho
                .replaceFirst("^/v1/planos/[0-9a-fA-F-]{36}", "")
                .replaceAll("/[0-9a-fA-F-]{36}", "")
                .replace('/', ' ')
                .trim();
        String verbo = switch (metodo) {
            case "POST" -> "Criou";
            case "PUT", "PATCH" -> "Alterou";
            case "DELETE" -> "Excluiu";
            default -> "Atualizou";
        };
        return verbo + " " + (recurso.isBlank() ? "o plano" : recurso) + ".";
    }

    private AtividadePlanoResponseDTO toResponse(AtividadePlanoEntity atividade) {
        return new AtividadePlanoResponseDTO(
                atividade.getId(),
                atividade.getAutor().getId(),
                atividade.getAutor().getUsuario().getNome(),
                atividade.getAcao(),
                atividade.getDescricao(),
                atividade.getCriadoEm()
        );
    }
}
