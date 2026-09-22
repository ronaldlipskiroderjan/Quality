package br.com.grupo5.Quality.service;

import br.com.grupo5.Quality.database.FeriadoPlanoEntity;
import br.com.grupo5.Quality.database.PlanoEntity;
import br.com.grupo5.Quality.database.enums.PermissaoPlano;
import br.com.grupo5.Quality.database.repository.FeriadoPlanoRepository;
import br.com.grupo5.Quality.dto.request.FeriadoPlanoRequestDTO;
import br.com.grupo5.Quality.dto.response.FeriadoPlanoResponseDTO;
import br.com.grupo5.Quality.exception.AlreadyExistsException;
import br.com.grupo5.Quality.exception.InvalidRequestException;
import br.com.grupo5.Quality.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarioPrazoService {

    static final ZoneId ZONA_DO_PLANO = ZoneId.of("America/Sao_Paulo");

    private final FeriadoPlanoRepository feriadoRepository;
    private final AcessoPlanoService acessoPlanoService;

    @Transactional(readOnly = true)
    public List<FeriadoPlanoResponseDTO> listar(
            Authentication auth,
            UUID planoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId);
        return feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FeriadoPlanoResponseDTO adicionar(
            Authentication auth,
            UUID planoId,
            FeriadoPlanoRequestDTO dto
    ) {
        PlanoEntity plano = acessoPlanoService.buscarPlano(
                auth,
                planoId,
                PermissaoPlano.EDITAR
        );
        feriadoRepository.findByPlanoIdAndData(planoId, dto.data())
                .ifPresent(feriado -> {
                    throw new AlreadyExistsException(
                            "Já existe um feriado cadastrado nesta data."
                    );
                });
        FeriadoPlanoEntity feriado = FeriadoPlanoEntity.builder()
                .plano(plano)
                .data(dto.data())
                .nome(dto.nome().trim())
                .build();
        return toResponse(feriadoRepository.save(feriado));
    }

    @Transactional
    public void remover(
            Authentication auth,
            UUID planoId,
            UUID feriadoId
    ) {
        acessoPlanoService.buscarPlano(auth, planoId, PermissaoPlano.EDITAR);
        FeriadoPlanoEntity feriado = feriadoRepository
                .findByIdAndPlanoId(feriadoId, planoId)
                .orElseThrow(() -> new NotFoundException(
                        "Feriado não encontrado."
                ));
        feriadoRepository.delete(feriado);
    }

    @Transactional(readOnly = true)
    public OffsetDateTime calcularPrazo(
            UUID planoId,
            OffsetDateTime inicio,
            int prazoHoras
    ) {
        if (prazoHoras <= 0) {
            throw new InvalidRequestException(
                    "O prazo deve ser de ao menos uma hora."
            );
        }
        Set<LocalDate> feriados = new HashSet<>();
        feriadoRepository.findAllByPlanoIdOrderByDataAsc(planoId)
                .stream()
                .map(FeriadoPlanoEntity::getData)
                .forEach(feriados::add);

        ZonedDateTime limite = inicio.atZoneSameInstant(ZONA_DO_PLANO);
        int diasUteis = prazoHoras / 24;
        int horasRestantes = prazoHoras % 24;

        for (int dia = 0; dia < diasUteis; dia++) {
            limite = proximoDiaUtil(limite, feriados);
        }
        limite = limite.plusHours(horasRestantes);
        while (!diaUtil(limite.toLocalDate(), feriados)) {
            limite = limite.plusDays(1);
        }
        return limite.toOffsetDateTime();
    }

    private ZonedDateTime proximoDiaUtil(
            ZonedDateTime data,
            Set<LocalDate> feriados
    ) {
        ZonedDateTime proxima = data.plusDays(1);
        while (!diaUtil(proxima.toLocalDate(), feriados)) {
            proxima = proxima.plusDays(1);
        }
        return proxima;
    }

    private boolean diaUtil(LocalDate data, Set<LocalDate> feriadosDoPlano) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY
                && dia != DayOfWeek.SUNDAY
                && !feriadosNacionais(data.getYear()).contains(data)
                && !feriadosDoPlano.contains(data);
    }

    private Set<LocalDate> feriadosNacionais(int ano) {
        Set<LocalDate> feriados = new HashSet<>(List.of(
                LocalDate.of(ano, 1, 1),
                dataPascoa(ano).minusDays(2),
                LocalDate.of(ano, 4, 21),
                LocalDate.of(ano, 5, 1),
                LocalDate.of(ano, 9, 7),
                LocalDate.of(ano, 10, 12),
                LocalDate.of(ano, 11, 2),
                LocalDate.of(ano, 11, 15),
                LocalDate.of(ano, 12, 25)
        ));
        if (ano >= 2024) {
            feriados.add(LocalDate.of(ano, 11, 20));
        }
        return feriados;
    }

    private LocalDate dataPascoa(int ano) {
        int a = ano % 19;
        int b = ano / 100;
        int c = ano % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(ano, mes, dia);
    }

    private FeriadoPlanoResponseDTO toResponse(FeriadoPlanoEntity feriado) {
        return new FeriadoPlanoResponseDTO(
                feriado.getId(),
                feriado.getData(),
                feriado.getNome()
        );
    }
}
