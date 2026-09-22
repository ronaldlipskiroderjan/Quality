package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ResolucaoNcEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResolucaoNcRepository
        extends JpaRepository<ResolucaoNcEntity, UUID> {

    List<ResolucaoNcEntity> findAllByNaoConformidadeId(UUID naoConformidadeId);

    Page<ResolucaoNcEntity>
            findAllByNaoConformidadeId(
                    UUID naoConformidadeId,
                    Pageable pageable
            );

    Optional<ResolucaoNcEntity> findByIdAndNaoConformidadeId(
            UUID resolucaoId,
            UUID naoConformidadeId
    );
}
