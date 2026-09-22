package br.com.grupo5.Quality.scheduler;

import br.com.grupo5.Quality.database.enums.StatusNaoConformidade;
import br.com.grupo5.Quality.database.repository.NaoConformidadeRepository;
import br.com.grupo5.Quality.service.VencimentoNaoConformidadeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrazoNaoConformidadeSchedulerTest {

    @Mock
    private NaoConformidadeRepository naoConformidadeRepository;
    @Mock
    private VencimentoNaoConformidadeService vencimentoService;

    @Test
    void deveProcessarTodasAsNcsVencidasMesmoSeUmaFalhar() {
        UUID primeiraId = UUID.randomUUID();
        UUID segundaId = UUID.randomUUID();
        when(naoConformidadeRepository.listarIdsComPrazoVencido(
                any(OffsetDateTime.class),
                anyCollection(),
                any(Pageable.class)
        )).thenReturn(List.of(primeiraId, segundaId));
        doThrow(new IllegalStateException("Falha simulada"))
                .when(vencimentoService)
                .processar(
                        org.mockito.ArgumentMatchers.eq(primeiraId),
                        any(OffsetDateTime.class)
                );

        scheduler().processar();

        verify(vencimentoService).processar(
                org.mockito.ArgumentMatchers.eq(primeiraId),
                any(OffsetDateTime.class)
        );
        verify(vencimentoService).processar(
                org.mockito.ArgumentMatchers.eq(segundaId),
                any(OffsetDateTime.class)
        );
    }

    @Test
    void naoDeveProcessarQuandoNaoHaPrazosVencidos() {
        when(naoConformidadeRepository.listarIdsComPrazoVencido(
                any(OffsetDateTime.class),
                anyCollection(),
                any(Pageable.class)
        )).thenReturn(List.of());

        scheduler().processar();

        verify(vencimentoService, never())
                .processar(any(), any());
    }


    @Test
    void deveMonitorarPrazosDosEscalonamentos() {
        when(naoConformidadeRepository.listarIdsComPrazoVencido(
                any(OffsetDateTime.class),
                anyCollection(),
                any(Pageable.class)
        )).thenReturn(List.of());

        scheduler().processar();

        org.mockito.ArgumentCaptor<Collection<StatusNaoConformidade>> captor =
                captorStatus();
        verify(naoConformidadeRepository).listarIdsComPrazoVencido(
                any(OffsetDateTime.class),
                captor.capture(),
                any(Pageable.class)
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                captor.getValue().contains(
                        StatusNaoConformidade.ESCALONADA_N1
                )
        );
        org.junit.jupiter.api.Assertions.assertTrue(
                captor.getValue().contains(
                        StatusNaoConformidade.ESCALONADA_N2
                )
        );
    }
    private PrazoNaoConformidadeScheduler scheduler() {
        return new PrazoNaoConformidadeScheduler(
                naoConformidadeRepository,
                vencimentoService,
                100
        );
    }

    @SuppressWarnings("unchecked")
    private Collection<StatusNaoConformidade> anyCollection() {
        return any(Collection.class);
    }

    @SuppressWarnings("unchecked")
    private org.mockito.ArgumentCaptor<Collection<StatusNaoConformidade>>
            captorStatus() {
        return org.mockito.ArgumentCaptor.forClass(Collection.class);
    }
}
