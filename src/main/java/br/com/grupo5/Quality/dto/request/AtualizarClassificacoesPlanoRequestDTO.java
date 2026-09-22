package br.com.grupo5.Quality.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AtualizarClassificacoesPlanoRequestDTO(
        @NotEmpty(message = "Informe as classificações do plano.")
        List<@Valid ConfiguracaoClassificacaoRequestDTO> classificacoes
) {
}
