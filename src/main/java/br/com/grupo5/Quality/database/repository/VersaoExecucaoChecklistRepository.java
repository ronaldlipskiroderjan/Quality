package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.VersaoExecucaoChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VersaoExecucaoChecklistRepository
        extends JpaRepository<VersaoExecucaoChecklistEntity, UUID> {

    List<VersaoExecucaoChecklistEntity>
            findAllByChecklistIdOrderByNumeroDesc(UUID checklistId);

    Optional<VersaoExecucaoChecklistEntity>
            findTopByChecklistIdOrderByNumeroDesc(UUID checklistId);

    Optional<VersaoExecucaoChecklistEntity>
            findByIdAndChecklistIdAndChecklistAuditoriaIdAndChecklistAuditoriaArtefatoIdAndChecklistAuditoriaArtefatoDocumentoPlanoId(
                    UUID versaoId,
                    UUID checklistId,
                    UUID auditoriaId,
                    UUID artefatoId,
                    UUID planoId
            );
}
