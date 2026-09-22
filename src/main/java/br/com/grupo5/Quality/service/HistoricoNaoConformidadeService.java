package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ComunicacaoNcEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
import br.com.grupo5.Quality.database.EncaminhamentoNcEntity;
import br.com.grupo5.Quality.database.NaoConformidadeEntity;
import br.com.grupo5.Quality.database.NotificacaoEntity;
import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import br.com.grupo5.Quality.database.enums.NivelEscalonamento;
import br.com.grupo5.Quality.database.enums.StatusComunicacao;
import br.com.grupo5.Quality.database.enums.StatusResolucao;
import br.com.grupo5.Quality.database.enums.TipoEventoNaoConformidade;
import br.com.grupo5.Quality.database.repository.ComunicacaoNcRepository;
import br.com.grupo5.Quality.database.repository.EscalonamentoNcRepository;
import br.com.grupo5.Quality.database.repository.EncaminhamentoNcRepository;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.database.repository.NotificacaoRepository;
import br.com.grupo5.Quality.database.repository.ResolucaoNcRepository;
import br.com.grupo5.Quality.dto.response.EventoNaoConformidadeResponseDTO;
import br.com.grupo5.Quality.dto.response.PaginaResponseDTO;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HistoricoNaoConformidadeService {

    private final NaoConformidadeRepository naoConformidadeRepository;
    private final ComunicacaoNcRepository comunicacaoRepository;
    private final ResolucaoNcRepository resolucaoRepository;
    private final EscalonamentoNcRepository escalonamentoRepository;
    private final EncaminhamentoNcRepository encaminhamentoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final AcessoNaoConformidadeService acessoNaoConformidadeService;

    @Transactional(readOnly = true)
    public PaginaResponseDTO<EventoNaoConformidadeResponseDTO> listar(
            Authentication auth,
            UUID planoId,
            UUID naoConformidadeId,
            Pageable pageable
    ) {
        acessoNaoConformidadeService.validarConsulta(auth, planoId, naoConformidadeId);
        NaoConformidadeEntity naoConformidade =
                naoConformidadeRepository
                        .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                                naoConformidadeId,
                                planoId
                        )
                        .orElseThrow(() -> new NotFoundException(
                                "Não conformidade não encontrada."
                        ));

        List<EventoNaoConformidadeResponseDTO> eventos = new ArrayList<>();
        eventos.add(eventoIdentificacao(naoConformidade));
        comunicacaoRepository.findAllByNaoConformidadeId(naoConformidadeId)
                .forEach(comunicacao -> eventos.add(eventoComunicacao(comunicacao)));
        encaminhamentoRepository.findAllByNaoConformidadeId(naoConformidadeId)
                .forEach(encaminhamento ->
                        eventos.add(eventoEncaminhamento(encaminhamento)));
        resolucaoRepository.findAllByNaoConformidadeId(naoConformidadeId)
                .forEach(resolucao -> adicionarEventosResolucao(eventos, resolucao));
        escalonamentoRepository.findAllByNaoConformidadeId(naoConformidadeId)
                .forEach(escalonamento -> eventos.add(eventoEscalonamento(escalonamento)));
        notificacaoRepository.findAllByNaoConformidadeId(naoConformidadeId)
                .forEach(notificacao -> eventos.add(eventoNotificacao(notificacao)));

        eventos.sort(Comparator
                .comparing(EventoNaoConformidadeResponseDTO::ocorridoEm)
                .thenComparing(evento -> evento.tipo().name()));

        int inicio = (int) Math.min(pageable.getOffset(), eventos.size());
        int fim = Math.min(inicio + pageable.getPageSize(), eventos.size());

        return PaginaResponseDTO.de(new PageImpl<>(
                eventos.subList(inicio, fim),
                pageable,
                eventos.size()
        ));
    }

    private EventoNaoConformidadeResponseDTO eventoIdentificacao(
            NaoConformidadeEntity naoConformidade
    ) {
        return new EventoNaoConformidadeResponseDTO(
                naoConformidade.getId(),
                TipoEventoNaoConformidade.NAO_CONFORMIDADE_IDENTIFICADA,
                naoConformidade.getIdentificadoEm(),
                "Não conformidade identificada",
                naoConformidade.getDescricao()
        );
    }

    private EventoNaoConformidadeResponseDTO eventoComunicacao(
            ComunicacaoNcEntity comunicacao
    ) {
        boolean enviada = comunicacao.getStatus() == StatusComunicacao.ENVIADA;
        OffsetDateTime ocorridoEm = enviada
                ? comunicacao.getEnviadaEm()
                : comunicacao.getUltimaTentativaEm();
        if (ocorridoEm == null) {
            ocorridoEm = comunicacao.getCriadaEm();
        }

        return new EventoNaoConformidadeResponseDTO(
                comunicacao.getId(),
                enviada
                        ? TipoEventoNaoConformidade.COMUNICACAO_ENVIADA
                        : TipoEventoNaoConformidade.COMUNICACAO_FALHOU,
                ocorridoEm,
                enviada
                        ? "Comunicação externa histórica enviada"
                        : "Falha histórica na comunicação externa",
                enviada
                        ? "Destinatário: " + comunicacao.getDestinatario()
                        : comunicacao.getDetalheErro()
        );
    }

    private void adicionarEventosResolucao(
            List<EventoNaoConformidadeResponseDTO> eventos,
            ResolucaoNcEntity resolucao
    ) {
        eventos.add(new EventoNaoConformidadeResponseDTO(
                resolucao.getId(),
                TipoEventoNaoConformidade.RESOLUCAO_INFORMADA,
                resolucao.getInformadaEm(),
                "Resolução informada",
                resolucao.getDescricao()
        ));

        if (resolucao.getStatus() == StatusResolucao.INFORMADA
                || resolucao.getValidadaEm() == null) {
            return;
        }

        boolean aprovada = resolucao.getStatus() == StatusResolucao.APROVADA;
        eventos.add(new EventoNaoConformidadeResponseDTO(
                resolucao.getId(),
                aprovada
                        ? TipoEventoNaoConformidade.RESOLUCAO_APROVADA
                        : TipoEventoNaoConformidade.AJUSTES_SOLICITADOS,
                resolucao.getValidadaEm(),
                aprovada ? "Resolução aprovada" : "Ajustes solicitados",
                resolucao.getObservacaoAuditor()
        ));
    }

    private EventoNaoConformidadeResponseDTO eventoEncaminhamento(
            EncaminhamentoNcEntity encaminhamento
    ) {
        return new EventoNaoConformidadeResponseDTO(
                encaminhamento.getId(),
                TipoEventoNaoConformidade.ENCAMINHAMENTO_EQUIPE,
                encaminhamento.getEncaminhadoEm(),
                "Encaminhada à equipe de resolução",
                encaminhamento.getTotalDestinatarios()
                        + " membro(s) da equipe foram notificados."
        );
    }

    private EventoNaoConformidadeResponseDTO eventoEscalonamento(
            EscalonamentoNcEntity escalonamento
    ) {
        boolean nivelUm = escalonamento.getNivel() == NivelEscalonamento.N1;
        return new EventoNaoConformidadeResponseDTO(
                escalonamento.getId(),
                nivelUm
                        ? TipoEventoNaoConformidade.ESCALONAMENTO_N1
                        : TipoEventoNaoConformidade.ESCALONAMENTO_N2,
                escalonamento.getEscalonadoEm(),
                nivelUm ? "Escalonamento de nível 1" : "Escalonamento de nível 2",
                escalonamento.getObservacao()
        );
    }

    private EventoNaoConformidadeResponseDTO eventoNotificacao(
            NotificacaoEntity notificacao
    ) {
        return new EventoNaoConformidadeResponseDTO(
                notificacao.getId(),
                TipoEventoNaoConformidade.NOTIFICACAO_GERADA,
                notificacao.getCriadaEm(),
                notificacao.getTitulo(),
                notificacao.getMensagem()
        );
    }
}
