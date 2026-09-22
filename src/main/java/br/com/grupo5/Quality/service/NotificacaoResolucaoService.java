package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.PapelPlano;
import br.com.grupo5.Quality.database.enums.StatusNotificacao;
import br.com.grupo5.Quality.database.enums.TipoNotificacao;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ParticipacaoPlanoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacaoResolucaoService {

    private final NotificacaoRepository notificacaoRepository;
    private final ParticipacaoPlanoRepository participacaoRepository;
    private final EscalonamentoNcRepository escalonamentoRepository;

    @Transactional
    public void notificarResolucaoInformada(
            UUID planoId,
            NaoConformidadeEntity naoConformidade,
            ResolucaoNcEntity resolucao,
            OffsetDateTime criadaEm
    ) {
        Map<UUID, ParticipacaoPlanoEntity> destinatarios =
                new LinkedHashMap<>();
        adicionarPorPapel(
                destinatarios,
                planoId,
                PapelPlano.AUDITOR_RESPONSAVEL_QUALIDADE
        );

        Set<NivelEscalonamento> niveis = escalonamentoRepository
                .findAllByNaoConformidadeId(naoConformidade.getId())
                .stream()
                .map(escalonamento -> escalonamento.getNivel())
                .collect(Collectors.toSet());

        if (niveis.contains(NivelEscalonamento.N2)) {
            adicionarPorPapel(destinatarios, planoId, PapelPlano.SUPERIOR_N1);
            adicionarPorPapel(destinatarios, planoId, PapelPlano.SUPERIOR_N2);
        } else if (niveis.contains(NivelEscalonamento.N1)) {
            adicionarPorPapel(destinatarios, planoId, PapelPlano.SUPERIOR_N1);
        }

        List<NotificacaoEntity> notificacoes = destinatarios.values().stream()
                .map(destinatario -> criarNotificacao(
                        destinatario,
                        naoConformidade,
                        resolucao,
                        criadaEm
                ))
                .toList();
        salvarNovas(notificacoes);
    }

    @Transactional
    public void notificarAjustesSolicitados(
            UUID planoId,
            NaoConformidadeEntity naoConformidade,
            ResolucaoNcEntity resolucao,
            OffsetDateTime criadaEm
    ) {
        Map<UUID, ParticipacaoPlanoEntity> destinatarios =
                new LinkedHashMap<>();
        ParticipacaoPlanoEntity responsavel =
                naoConformidade.getResponsavel();

        if (responsavel != null && responsavel.isAtivo()) {
            destinatarios.put(responsavel.getId(), responsavel);
        } else {
            adicionarPorPapel(
                    destinatarios,
                    planoId,
                    PapelPlano.MEMBRO_EQUIPE_RESOLUCAO
            );
        }

        List<NotificacaoEntity> notificacoes = destinatarios.values().stream()
                .map(destinatario -> criarNotificacaoAjustes(
                        destinatario,
                        naoConformidade,
                        resolucao,
                        criadaEm
                ))
                .toList();
        salvarNovas(notificacoes);
    }

    private void adicionarPorPapel(
            Map<UUID, ParticipacaoPlanoEntity> destinatarios,
            UUID planoId,
            PapelPlano papel
    ) {
        participacaoRepository.buscarPorPapelNoPlano(planoId, papel)
                .forEach(participacao ->
                        destinatarios.put(participacao.getId(), participacao));
    }

    private NotificacaoEntity criarNotificacao(
            ParticipacaoPlanoEntity destinatario,
            NaoConformidadeEntity naoConformidade,
            ResolucaoNcEntity resolucao,
            OffsetDateTime criadaEm
    ) {
        return NotificacaoEntity.builder()
                .destinatario(destinatario)
                .naoConformidade(naoConformidade)
                .chaveEvento(
                        "RESOLUCAO_INFORMADA:"
                                + resolucao.getId()
                                + ":"
                                + destinatario.getId()
                )
                .tipo(TipoNotificacao.RESOLUCAO_INFORMADA)
                .titulo("Resolução informada")
                .mensagem(
                        "A equipe de resolução informou uma correção para a não conformidade. Acesse para acompanhar e revisar."
                )
                .status(StatusNotificacao.NAO_LIDA)
                .criadaEm(criadaEm)
                .build();
    }

    private NotificacaoEntity criarNotificacaoAjustes(
            ParticipacaoPlanoEntity destinatario,
            NaoConformidadeEntity naoConformidade,
            ResolucaoNcEntity resolucao,
            OffsetDateTime criadaEm
    ) {
        return NotificacaoEntity.builder()
                .destinatario(destinatario)
                .naoConformidade(naoConformidade)
                .chaveEvento(
                        "AJUSTES_RESOLUCAO_SOLICITADOS:"
                                + resolucao.getId()
                                + ":"
                                + destinatario.getId()
                )
                .tipo(TipoNotificacao.AJUSTES_RESOLUCAO_SOLICITADOS)
                .titulo("Ajustes solicitados na resolução")
                .mensagem(
                        "O auditor solicitou ajustes na resolução da não conformidade. Acesse para consultar a orientação e enviar uma nova correção."
                )
                .status(StatusNotificacao.NAO_LIDA)
                .criadaEm(criadaEm)
                .build();
    }

    private void salvarNovas(List<NotificacaoEntity> notificacoes) {
        List<NotificacaoEntity> novas = notificacoes.stream()
                .filter(notificacao -> !notificacaoRepository
                        .existsByChaveEvento(notificacao.getChaveEvento()))
                .toList();
        if (!novas.isEmpty()) {
            notificacaoRepository.saveAll(novas);
        }
    }
}
