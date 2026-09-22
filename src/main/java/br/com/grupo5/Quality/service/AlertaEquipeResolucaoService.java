package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertaEquipeResolucaoService {

    private final NaoConformidadeRepository naoConformidadeRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional
    public int emitir(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId
    ) {
        ParticipacaoPlanoEntity superior =
                acessoPlanoService.buscarParticipacao(auth, planoId);
        NivelEscalonamento nivel = validarSuperior(superior);
        NaoConformidadeEntity naoConformidade =
                naoConformidadeRepository.buscarParaAtualizacao(
                        naoConformidadeId,
                        planoId
                ).orElseThrow(() -> new NotFoundException(
                        "Não conformidade não encontrada."
                ));
        validarEscalonamentoDisponivel(naoConformidade, nivel);

        List<ParticipacaoPlanoEntity> equipe =
                participacaoRepository.buscarPorPapelNoPlano(
                        planoId,
                        PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
                );
        if (equipe.isEmpty()) {
            throw new InvalidRequestException(
                    "O plano não possui equipe de resolução para receber o alerta."
            );
        }

        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        int totalCriado = 0;
        for (ParticipacaoPlanoEntity membro : equipe) {
            String chave = "ALERTA_SUPERIOR:" + naoConformidadeId
                    + ":" + nivel.name() + ":" + membro.getId();
            if (notificacaoRepository.existsByChaveEvento(chave)) {
                continue;
            }
            notificacaoRepository.save(NotificacaoEntity.builder()
                    .destinatario(membro)
                    .naoConformidade(naoConformidade)
                    .chaveEvento(chave)
                    .tipo(TipoNotificacao.ALERTA_SUPERIOR_EQUIPE_RESOLUCAO)
                    .titulo("Alerta do superior " + nivel.name()
                            + ": prazo excedido")
                    .mensagem("O superior " + nivel.name()
                            + " advertiu a equipe de resolução de que o prazo "
                            + "desta não conformidade foi excedido.")
                    .status(StatusNotificacao.NAO_LIDA)
                    .criadaEm(agora)
                    .build());
            totalCriado++;
        }
        return totalCriado;
    }

    private NivelEscalonamento validarSuperior(
            ParticipacaoPlanoEntity participacao
    ) {
        if (participacao.possuiPapel(PapelPlano.SUPERIOR_N1)) {
            return NivelEscalonamento.N1;
        }
        if (participacao.possuiPapel(PapelPlano.SUPERIOR_N2)) {
            return NivelEscalonamento.N2;
        }
        throw new AccessDeniedException(
                "Somente o superior N1 ou N2 pode alertar a equipe de resolução."
        );
    }

    private void validarEscalonamentoDisponivel(
            NaoConformidadeEntity naoConformidade,
            NivelEscalonamento nivel
    ) {
        StatusNaoConformidade status = naoConformidade.getStatus();
        boolean permitido = nivel == NivelEscalonamento.N1
                ? status == StatusNaoConformidade.ESCALONADA_N1
                : status == StatusNaoConformidade.ESCALONADA_N2
                        || status == StatusNaoConformidade.VENCIDA_N2;
        if (!permitido) {
            throw new InvalidRequestException(
                    "O alerta só pode ser emitido pelo superior responsável "
                            + "pelo nível atual do escalonamento."
            );
        }
    }
}
