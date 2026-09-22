package br.com.grupo5.Quality.scheduler;

import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.service.VencimentoNaoConformidadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class PrazoNaoConformidadeScheduler {

    private static final Set<StatusNaoConformidade> STATUS_MONITORADOS =
            EnumSet.of(
                    StatusNaoConformidade.ENVIADA,
                    StatusNaoConformidade.EM_TRATAMENTO,
                    StatusNaoConformidade.ESCALONADA_N1,
                    StatusNaoConformidade.ESCALONADA_N2
            );

    private final NaoConformidadeRepository naoConformidadeRepository;
    private final VencimentoNaoConformidadeService vencimentoService;
    private final int tamanhoLote;

    public PrazoNaoConformidadeScheduler(
            NaoConformidadeRepository naoConformidadeRepository,
            VencimentoNaoConformidadeService vencimentoService,
            @Value("${app.agendamento.prazos.tamanho-lote:100}")
            int tamanhoLote
    ) {
        this.naoConformidadeRepository = naoConformidadeRepository;
        this.vencimentoService = vencimentoService;
        this.tamanhoLote = tamanhoLote;
    }

    @Scheduled(
            fixedDelayString = "${app.agendamento.prazos.intervalo-ms:60000}",
            initialDelayString = "${app.agendamento.prazos.atraso-inicial-ms:30000}"
    )
    public void processar() {
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);
        var ids = naoConformidadeRepository.listarIdsComPrazoVencido(
                agora,
                STATUS_MONITORADOS,
                PageRequest.of(0, Math.max(1, tamanhoLote))
        );

        for (UUID naoConformidadeId : ids) {
            try {
                vencimentoService.processar(naoConformidadeId, agora);
            } catch (RuntimeException exception) {
                log.error(
                        "Falha ao processar o vencimento da NC {}.",
                        naoConformidadeId,
                        exception
                );
            }
        }
    }
}
