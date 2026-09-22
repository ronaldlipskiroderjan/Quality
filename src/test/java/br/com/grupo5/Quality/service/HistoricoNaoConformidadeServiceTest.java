package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.ComunicacaoNcEntity;
import br.com.grupo5.Quality.database.EscalonamentoNcEntity;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoNaoConformidadeServiceTest {

    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private ComunicacaoNcRepository comunicacaoRepository;
    @Mock
    private ResolucaoNcRepository resolucaoRepository;
    @Mock
    private EscalonamentoNcRepository escalonamentoRepository;
    @Mock
    private EncaminhamentoNcRepository encaminhamentoRepository;
    @Mock
    private NotificacaoRepository notificacaoRepository;
    @Mock
    private AcessoNaoConformidadeService acessoNaoConformidadeService;
    @Mock
    private Authentication auth;

    @Test
    void deveOrdenarTodosOsEventosCronologicamente() {
        UUID planoId = UUID.randomUUID();
        UUID naoConformidadeId = UUID.randomUUID();
        OffsetDateTime inicio = OffsetDateTime.of(
                2026, 9, 14, 10, 0, 0, 0, ZoneOffset.UTC
        );

        NaoConformidadeEntity naoConformidade = NaoConformidadeEntity.builder()
                .id(naoConformidadeId)
                .identificadoEm(inicio)
                .descricao("Falha identificada")
                .build();
        ComunicacaoNcEntity comunicacao = ComunicacaoNcEntity.builder()
                .id(UUID.randomUUID())
                .status(StatusComunicacao.ENVIADA)
                .enviadaEm(inicio.plusHours(1))
                .destinatario("responsavel@quality.test")
                .build();
        ResolucaoNcEntity resolucao = ResolucaoNcEntity.builder()
                .id(UUID.randomUUID())
                .status(StatusResolucao.APROVADA)
                .descricao("Falha corrigida")
                .informadaEm(inicio.plusHours(2))
                .validadaEm(inicio.plusHours(4))
                .build();
        EscalonamentoNcEntity escalonamento = EscalonamentoNcEntity.builder()
                .id(UUID.randomUUID())
                .nivel(NivelEscalonamento.N1)
                .escalonadoEm(inicio.plusHours(3))
                .build();
        NotificacaoEntity notificacao = NotificacaoEntity.builder()
                .id(UUID.randomUUID())
                .criadaEm(inicio.plusHours(5))
                .titulo("Prazo vencido")
                .mensagem("Notificação enviada ao auditor")
                .build();

        when(naoConformidadeRepository
                .findByIdAndRespostaAuditoriaArtefatoDocumentoPlanoId(
                        naoConformidadeId,
                        planoId
                )).thenReturn(Optional.of(naoConformidade));
        when(comunicacaoRepository.findAllByNaoConformidadeId(naoConformidadeId))
                .thenReturn(List.of(comunicacao));
        when(resolucaoRepository.findAllByNaoConformidadeId(naoConformidadeId))
                .thenReturn(List.of(resolucao));
        when(escalonamentoRepository.findAllByNaoConformidadeId(naoConformidadeId))
                .thenReturn(List.of(escalonamento));
        when(notificacaoRepository.findAllByNaoConformidadeId(naoConformidadeId))
                .thenReturn(List.of(notificacao));

        PaginaResponseDTO<EventoNaoConformidadeResponseDTO> response =
                service().listar(
                        auth,
                        planoId,
                        naoConformidadeId,
                        PageRequest.of(0, 15)
                );

        assertEquals(6, response.totalElementos());
        assertEquals(List.of(
                        TipoEventoNaoConformidade.NAO_CONFORMIDADE_IDENTIFICADA,
                        TipoEventoNaoConformidade.COMUNICACAO_ENVIADA,
                        TipoEventoNaoConformidade.RESOLUCAO_INFORMADA,
                        TipoEventoNaoConformidade.ESCALONAMENTO_N1,
                        TipoEventoNaoConformidade.RESOLUCAO_APROVADA,
                        TipoEventoNaoConformidade.NOTIFICACAO_GERADA
                ),
                response.conteudo().stream()
                        .map(EventoNaoConformidadeResponseDTO::tipo)
                        .toList());
    }

    private HistoricoNaoConformidadeService service() {
        return new HistoricoNaoConformidadeService(
                naoConformidadeRepository,
                comunicacaoRepository,
                resolucaoRepository,
                escalonamentoRepository,
                encaminhamentoRepository,
                notificacaoRepository,
                acessoNaoConformidadeService
        );
    }
}
