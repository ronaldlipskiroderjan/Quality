package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.EncaminhamentoNcEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EncaminhamentoNcRepository
        extends JpaRepository<EncaminhamentoNcEntity, UUID> {

    Optional<EncaminhamentoNcEntity>
            findByNaoConformidadeIdAndChaveIdempotencia(
                    UUID naoConformidadeId,
                    String chaveIdempotencia
            );

    List<EncaminhamentoNcEntity> findAllByNaoConformidadeId(
            UUID naoConformidadeId
    );

    boolean existsByNaoConformidadeId(UUID naoConformidadeId);
}
