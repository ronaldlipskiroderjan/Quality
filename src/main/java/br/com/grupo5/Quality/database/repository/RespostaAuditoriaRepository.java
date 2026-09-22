package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.RespostaAuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RespostaAuditoriaRepository
        extends JpaRepository<RespostaAuditoriaEntity, UUID> {

    Optional<RespostaAuditoriaEntity> findByAuditoriaIdAndItemId(
            UUID auditoriaId,
            UUID itemId
    );

    Optional<RespostaAuditoriaEntity>
            findByIdAndAuditoriaArtefatoDocumentoPlanoId(
                    UUID respostaId,
                    UUID planoId
            );
}
