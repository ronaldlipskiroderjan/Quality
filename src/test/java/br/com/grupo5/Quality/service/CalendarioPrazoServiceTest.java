package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.FeriadoPlanoEntity;
import br.com.grupo5.Quality.database.repository.FeriadoPlanoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarioPrazoServiceTest {

    private static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    @Mock
    private FeriadoPlanoRepository feriadoRepository;
    @Mock
    private AcessoPlanoService acessoPlanoService;

    @Test
    void devePularFimDeSemanaAoSomarDoisDiasUteis() {
        UUID planoId = UUID.randomUUID();
        OffsetDateTime sexta = OffsetDateTime.of(
                2026, 9, 18, 10, 0, 0, 0, BRASILIA
        );
        when(feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId))
                .thenReturn(List.of());

        OffsetDateTime prazo = service().calcularPrazo(
                planoId,
                sexta,
                48
        );

        assertEquals(
                OffsetDateTime.of(2026, 9, 22, 10, 0, 0, 0, BRASILIA),
                prazo
        );
    }

    @Test
    void devePularFeriadoDoPlanoDepoisDoFimDeSemana() {
        UUID planoId = UUID.randomUUID();
        OffsetDateTime sexta = OffsetDateTime.of(
                2026, 9, 18, 10, 0, 0, 0, BRASILIA
        );
        FeriadoPlanoEntity segundaFeira = FeriadoPlanoEntity.builder()
                .data(LocalDate.of(2026, 9, 21))
                .nome("Feriado municipal")
                .build();
        when(feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId))
                .thenReturn(List.of(segundaFeira));

        OffsetDateTime prazo = service().calcularPrazo(
                planoId,
                sexta,
                48
        );

        assertEquals(
                OffsetDateTime.of(2026, 9, 23, 10, 0, 0, 0, BRASILIA),
                prazo
        );
    }

    @Test
    void deveConsiderarFeriadoNacionalAutomaticamente() {
        UUID planoId = UUID.randomUUID();
        OffsetDateTime sexta = OffsetDateTime.of(
                2026, 9, 4, 14, 30, 0, 0, BRASILIA
        );
        when(feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId))
                .thenReturn(List.of());

        OffsetDateTime prazo = service().calcularPrazo(
                planoId,
                sexta,
                24
        );

        assertEquals(
                OffsetDateTime.of(2026, 9, 8, 14, 30, 0, 0, BRASILIA),
                prazo
        );
    }

    @Test
    void deveConsiderarSextaFeiraDaPaixaoAutomaticamente() {
        UUID planoId = UUID.randomUUID();
        OffsetDateTime quinta = OffsetDateTime.of(
                2026, 4, 2, 9, 0, 0, 0, BRASILIA
        );
        when(feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId))
                .thenReturn(List.of());

        OffsetDateTime prazo = service().calcularPrazo(
                planoId,
                quinta,
                24
        );

        assertEquals(
                OffsetDateTime.of(2026, 4, 6, 9, 0, 0, 0, BRASILIA),
                prazo
        );
    }

    @Test
    void deveLevarHorasRestantesParaOProximoDiaUtil() {
        UUID planoId = UUID.randomUUID();
        OffsetDateTime sexta = OffsetDateTime.of(
                2026, 9, 18, 20, 0, 0, 0, BRASILIA
        );
        when(feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId))
                .thenReturn(List.of());

        OffsetDateTime prazo = service().calcularPrazo(
                planoId,
                sexta,
                10
        );

        assertEquals(
                OffsetDateTime.of(2026, 9, 21, 6, 0, 0, 0, BRASILIA),
                prazo
        );
    }

    private CalendarioPrazoService service() {
        return new CalendarioPrazoService(
                feriadoRepository,
                acessoPlanoService
        );
    }
}
