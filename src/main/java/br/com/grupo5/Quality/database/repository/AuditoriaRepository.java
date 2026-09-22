package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.StatusAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, UUID> {

    Page<AuditoriaEntity> findAllByArtefatoId(
            UUID artefatoId,
            Pageable pageable
    );

    Optional<AuditoriaEntity>
            findByIdAndArtefatoIdAndArtefatoDocumentoPlanoId(
                    UUID auditoriaId,
                    UUID artefatoId,
                    UUID planoId
            );

    Optional<AuditoriaEntity> findByArtefatoIdAndArtefatoDocumentoPlanoId(
            UUID artefatoId,
            UUID planoId
    );

    boolean existsByArtefatoIdAndStatusIn(
            UUID artefatoId,
            Collection<StatusAuditoria> status
    );

    boolean existsByArtefatoId(UUID artefatoId);

    boolean existsByAuditorId(UUID participanteId);

    @Modifying
    @Query("""
            update AuditoriaEntity auditoria
            set auditoria.auditor = :novoAuditor
            where auditoria.auditor.id = :auditorAnteriorId
              and auditoria.status not in :statusFinais
            """)
    int reatribuirEmExecucao(
            @Param("auditorAnteriorId") UUID auditorAnteriorId,
            @Param("novoAuditor") ParticipacaoPlanoEntity novoAuditor,
            @Param("statusFinais") Collection<StatusAuditoria> statusFinais
    );
}
