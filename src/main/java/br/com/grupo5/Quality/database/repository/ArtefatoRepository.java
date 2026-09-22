package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ArtefatoEntity;
import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import br.com.grupo5.Quality.database.enums.StatusArtefato;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ArtefatoRepository extends JpaRepository<ArtefatoEntity, UUID> {

    Page<ArtefatoEntity> findAllByDocumentoPlanoId(
            UUID planoId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "documento.plano",
            "auditor.usuario",
            "auditoria"
    })
    Page<ArtefatoEntity> findAllByAuditorUsuarioEmailIgnoreCaseAndAuditorAtivoTrue(
            String email,
            Pageable pageable
    );

    Optional<ArtefatoEntity> findByIdAndDocumentoPlanoId(
            UUID artefatoId,
            UUID planoId
    );

    boolean existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCase(
            UUID documentoId,
            String nome,
            String versao
    );

    boolean existsByDocumentoIdAndNomeIgnoreCaseAndVersaoIgnoreCaseAndIdNot(
            UUID documentoId,
            String nome,
            String versao,
            UUID artefatoId
    );

    boolean existsByAuditorId(UUID participanteId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from artefatos where id = :artefatoId", nativeQuery = true)
    void excluirComDependencias(@Param("artefatoId") UUID artefatoId);

    @Modifying
    @Query("""
            update ArtefatoEntity artefato
            set artefato.auditor = :novoAuditor
            where artefato.auditor.id = :auditorAnteriorId
              and artefato.status not in :statusFinais
            """)
    int reatribuirEmExecucao(
            @Param("auditorAnteriorId") UUID auditorAnteriorId,
            @Param("novoAuditor") ParticipacaoPlanoEntity novoAuditor,
            @Param("statusFinais") Collection<StatusArtefato> statusFinais
    );
}
