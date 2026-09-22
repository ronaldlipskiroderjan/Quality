package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.enums.Classificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<DocumentoEntity, UUID> {

    Page<DocumentoEntity> findAllByPlanoId(
            UUID planoId,
            Pageable pageable
    );

    Page<DocumentoEntity> findAllByPlanoIdAndClassificacao(
            UUID planoId,
            Classificacao classificacao,
            Pageable pageable
    );

    Optional<DocumentoEntity> findByIdAndPlanoId(UUID id, UUID planoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "delete from documentos where id = :documentoId", nativeQuery = true)
    void excluirComDependencias(@Param("documentoId") UUID documentoId);
}
