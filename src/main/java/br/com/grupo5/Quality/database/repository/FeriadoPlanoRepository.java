package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.FeriadoPlanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeriadoPlanoRepository
        extends JpaRepository<FeriadoPlanoEntity, UUID> {

    List<FeriadoPlanoEntity> findAllByPlanoIdOrderByDataAsc(UUID planoId);

    Optional<FeriadoPlanoEntity> findByPlanoIdAndData(
            UUID planoId,
            LocalDate data
    );

    Optional<FeriadoPlanoEntity> findByIdAndPlanoId(UUID id, UUID planoId);
}
