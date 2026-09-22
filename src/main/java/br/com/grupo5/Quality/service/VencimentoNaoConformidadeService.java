package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VencimentoNaoConformidadeService {

    private static final Set<StatusNaoConformidade> STATUS_MONITORADOS =
            EnumSet.of(
                    StatusNaoConformidade.ENVIADA,
                    StatusNaoConformidade.EM_TRATAMENTO,
                    StatusNaoConformidade.ESCALONADA_N1,
                    StatusNaoConformidade.ESCALONADA_N2
            );
    private static final String TITULO_PRAZO_N2 =
            "Prazo do escalonamento N2 vencido";

    private final NaoConformidadeRepository naoConformidadeRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final EscalonamentoNcRepository escalonamentoRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final CalendarioPrazoService calendarioPrazoService;

    @Transactional
    public boolean processar(UUID naoConformidadeId, OffsetDateTime agora) {
        NaoConformidadeEntity naoConformidade = naoConformidadeRepository
                .buscarPorIdParaAtualizacao(naoConformidadeId)
                .orElse(null);

        if (!estaVencida(naoConformidade, agora)) {
            return false;
        }

        if (naoConformidade.getStatus() == StatusNaoConformidade.ESCALONADA_N2) {
            registrarVencimentoFinal(naoConformidade, agora);
        } else {
            NivelEscalonamento nivel =
                    naoConformidade.getStatus()
                                    == StatusNaoConformidade.ESCALONADA_N1
                            ? NivelEscalonamento.N2
                            : NivelEscalonamento.N1;
            escalonar(naoConformidade, nivel, agora);
        }
        naoConformidadeRepository.save(naoConformidade);
        return true;
    }

    private boolean estaVencida(
            NaoConformidadeEntity naoConformidade,
            OffsetDateTime agora
    ) {
        return naoConformidade != null
                && STATUS_MONITORADOS.contains(naoConformidade.getStatus())
                && naoConformidade.getPrazoEm() != null
                && !naoConformidade.getPrazoEm().isAfter(agora);
    }

    private void escalonar(
            NaoConformidadeEntity naoConformidade,
            NivelEscalonamento nivel,
            OffsetDateTime agora
    ) {
        ParticipacaoPlanoEntity auditor = naoConformidade.getResposta()
                .getAuditoria()
                .getAuditor();
        UUID planoId = auditor.getPlano().getId();
        PapelPlano papel = nivel == NivelEscalonamento.N1
                ? PapelPlano.SUPERIOR_N1
                : PapelPlano.SUPERIOR_N2;
        ParticipacaoPlanoEntity superior = buscarSuperior(planoId, papel);
        int prazoHoras = naoConformidade.getPrazoResolucaoHoras();
        if (prazoHoras < 1) {
            throw new InvalidRequestException(
                    "A classificação da NC não possui prazo válido para escalonamento."
            );
        }
        OffsetDateTime novoPrazo = calendarioPrazoService.calcularPrazo(
                planoId,
                agora,
                prazoHoras
        );

        EscalonamentoNcEntity escalonamento =
                EscalonamentoNcEntity.builder()
                        .naoConformidade(naoConformidade)
                        .chaveIdempotencia(
                                "AUTO-" + nivel.name() + "-" + naoConformidade.getId()
                        )
                        .nivel(nivel)
                        .responsavel(superior)
                        .auditor(auditor)
                        .observacao("Escalonamento automático por prazo excedido.")
                        .prazoHoras(prazoHoras)
                        .escalonadoEm(agora)
                        .prazoOriginalEm(novoPrazo)
                        .prazoEm(novoPrazo)
                        .build();
        escalonamentoRepository.saveAndFlush(escalonamento);

        notificacaoRepository.save(NotificacaoEntity.builder()
                .destinatario(superior)
                .naoConformidade(naoConformidade)
                .chaveEvento("ESCALONAMENTO_AUTO_" + nivel.name()
                        + ":" + naoConformidade.getId())
                .tipo(nivel == NivelEscalonamento.N1
                        ? TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N1
                        : TipoNotificacao.NAO_CONFORMIDADE_ESCALONADA_N2)
                .titulo("Não conformidade escalonada automaticamente para "
                        + nivel.name())
                .mensagem("O prazo anterior foi excedido. A não conformidade "
                        + "agora requer acompanhamento do superior "
                        + nivel.name() + ".")
                .status(StatusNotificacao.NAO_LIDA)
                .criadaEm(agora)
                .build());

        naoConformidade.setStatus(nivel == NivelEscalonamento.N1
                ? StatusNaoConformidade.ESCALONADA_N1
                : StatusNaoConformidade.ESCALONADA_N2);
        naoConformidade.setPrazoEm(novoPrazo);
        naoConformidade.setAtualizadoEm(agora);
    }

    private ParticipacaoPlanoEntity buscarSuperior(
            UUID planoId,
            PapelPlano papel
    ) {
        List<ParticipacaoPlanoEntity> superiores =
                participacaoRepository.buscarPorPapelNoPlano(planoId, papel);
        if (superiores.size() != 1) {
            throw new InvalidRequestException(
                    "Defina exatamente um "
                            + (papel == PapelPlano.SUPERIOR_N1 ? "superior N1" : "superior N2")
                            + " no plano antes do vencimento."
            );
        }
        return superiores.getFirst();
    }

    private void registrarVencimentoFinal(
            NaoConformidadeEntity naoConformidade,
            OffsetDateTime agora
    ) {
        List<ParticipacaoPlanoEntity> destinatarios = List.of(
                naoConformidade.getResposta().getAuditoria().getAuditor(),
                buscarSuperior(
                        naoConformidade.getResposta().getAuditoria()
                                .getAuditor().getPlano().getId(),
                        PapelPlano.SUPERIOR_N2
                )
        );
        for (ParticipacaoPlanoEntity destinatario : destinatarios) {
            String chave = "PRAZO_ESCALONAMENTO_N2_VENCIDO:"
                    + naoConformidade.getId() + ":" + destinatario.getId();
            if (!notificacaoRepository.existsByChaveEvento(chave)) {
                notificacaoRepository.save(NotificacaoEntity.builder()
                        .destinatario(destinatario)
                        .naoConformidade(naoConformidade)
                        .chaveEvento(chave)
                        .tipo(TipoNotificacao.PRAZO_ESCALONAMENTO_N2_VENCIDO)
                        .titulo(TITULO_PRAZO_N2)
                        .mensagem("O prazo do N2 terminou sem uma resolução informada.")
                        .status(StatusNotificacao.NAO_LIDA)
                        .criadaEm(agora)
                        .build());
            }
        }
        naoConformidade.setStatus(StatusNaoConformidade.VENCIDA_N2);
        naoConformidade.setAtualizadoEm(agora);
    }
}
