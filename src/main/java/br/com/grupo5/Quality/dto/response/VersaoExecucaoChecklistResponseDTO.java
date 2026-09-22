package br.com.grupo5.Quality.dto.response;

import br.com.grupo5.Quality.database.enums.TipoVersaoExecucaoChecklist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VersaoExecucaoChecklistResponseDTO(
        UUID id,
        UUID checklistId,
        int numero,
        TipoVersaoExecucaoChecklist tipo,
        String observacao,
        String autorNome,
        LocalDateTime criadoEm,
        List<ItemVersaoExecucaoResponseDTO> itens
) {
}
