package br.com.grupo5.Quality.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Set;

public record ArtefatoRequestDTO(
        @NotNull(message = "O documento é obrigatório.")
        UUID documentoId,

        @NotNull(message = "O auditor é obrigatório.")
        UUID auditorParticipacaoId,

        @NotEmpty(message = "Selecione ao menos um documento de referência.")
        Set<UUID> documentoReferenciaIds,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 150, message = "O nome deve possuir no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "A versão é obrigatória.")
        @Size(max = 30, message = "A versão deve possuir no máximo 30 caracteres.")
        String versao,

        @NotNull(message = "A data planejada é obrigatória.")
        @FutureOrPresent(message = "A data planejada não pode estar no passado.")
        LocalDate dataPlanejada
) {
}
