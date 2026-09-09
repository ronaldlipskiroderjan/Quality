package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.ParticipacaoPlanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipacaoPlanoRepository
        extends JpaRepository<ParticipacaoPlanoEntity, UUID> {

    List<ParticipacaoPlanoEntity>
            findAllByUsuarioEmailIgnoreCaseOrderByPlanoCriadoEmDesc(String email);

    List<ParticipacaoPlanoEntity> findAllByPlanoIdOrderByCriadoEmAsc(UUID planoId);

    Optional<ParticipacaoPlanoEntity> findByPlanoIdAndUsuarioEmailIgnoreCase(
            UUID planoId,
            String email
    );

    Optional<ParticipacaoPlanoEntity> findByIdAndPlanoId(
            UUID participanteId,
            UUID planoId
    );

    boolean existsByPlanoIdAndUsuarioId(UUID planoId, UUID usuarioId);
}
