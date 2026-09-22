package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ChecklistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface ChecklistRepository extends JpaRepository<ChecklistEntity, UUID> {

    Page<ChecklistEntity> findAllByAuditoriaId(
            UUID auditoriaId,
            Pageable pageable
    );

    Optional<ChecklistEntity>
            findByIdAndAuditoriaIdAndAuditoriaArtefatoIdAndAuditoriaArtefatoDocumentoPlanoId(
                    UUID checklistId,
                    UUID auditoriaId,
                    UUID artefatoId,
                    UUID planoId
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select checklist
            from ChecklistEntity checklist
            where checklist.id = :checklistId
              and checklist.auditoria.id = :auditoriaId
              and checklist.auditoria.artefato.id = :artefatoId
              and checklist.auditoria.artefato.documento.plano.id = :planoId
            """)
    Optional<ChecklistEntity> buscarParaAtualizacao(
            @Param("planoId") UUID planoId,
            @Param("artefatoId") UUID artefatoId,
            @Param("auditoriaId") UUID auditoriaId,
            @Param("checklistId") UUID checklistId
    );

}
