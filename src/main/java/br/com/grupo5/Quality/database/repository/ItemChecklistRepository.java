package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ItemChecklistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ItemChecklistRepository
        extends JpaRepository<ItemChecklistEntity, UUID> {

    Optional<ItemChecklistEntity> findByIdAndChecklistId(
            UUID itemId,
            UUID checklistId
    );

    Optional<ItemChecklistEntity> findByIdAndChecklistAuditoriaId(
            UUID itemId,
            UUID auditoriaId
    );

    boolean existsByChecklistIdAndOrdem(UUID checklistId, int ordem);

    boolean existsByChecklistIdAndOrdemAndIdNot(
            UUID checklistId,
            int ordem,
            UUID itemId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ItemChecklistEntity item
            set item.ordem = item.ordem - 1
            where item.checklist.id = :checklistId
              and item.ordem > :ordemRemovida
            """)
    int compactarOrdensApos(
            @Param("checklistId") UUID checklistId,
            @Param("ordemRemovida") int ordemRemovida
    );
}
