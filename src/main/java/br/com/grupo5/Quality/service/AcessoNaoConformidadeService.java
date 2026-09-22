package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.EncaminhamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcessoNaoConformidadeService {

    private final ParticipacaoPlanoRepository participacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final NaoConformidadeRepository naoConformidadeRepository;
    private final EncaminhamentoNcRepository encaminhamentoRepository;

    @Transactional(readOnly = true)
    public void validarConsulta(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId
    ) {
        ParticipacaoPlanoEntity participacao = participacaoRepository
                .findByPlanoIdAndUsuarioEmailIgnoreCase(planoId, auth.getName())
                .orElseThrow(this::naoEncontrada);

        boolean acessaPlano = participacao.possuiPermissao(
                PermissaoPlano.VISUALIZAR
        );
        StatusNaoConformidade status = naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        naoConformidadeId,
                        planoId
                )
                .orElseThrow(this::naoEncontrada)
                .getStatus();
        boolean membroDaEquipe = participacao.possuiPapel(
                PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
        ) && status != StatusNaoConformidade.RASCUNHO
                && encaminhamentoRepository.existsByNaoConformidadeId(
                        naoConformidadeId
                );
        boolean superiorNotificado = (
                participacao.possuiPapel(PapelPlano.SUPERIOR_N1)
                        || participacao.possuiPapel(PapelPlano.SUPERIOR_N2)
        ) && notificacaoRepository
                .existsByNaoConformidadeIdAndDestinatarioId(
                        naoConformidadeId,
                        participacao.getId()
                );

        if (!acessaPlano && !membroDaEquipe && !superiorNotificado) {
            throw naoEncontrada();
        }
    }

    private NotFoundException naoEncontrada() {
        return new NotFoundException("Não conformidade não encontrada.");
    }
}
