package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.DocumentoEntity;
import br.com.grupo5.Quality.database.enums.Classificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<DocumentoEntity, UUID> {

    List<DocumentoEntity> findAllByPlanoIdOrderByNomeAsc(UUID planoId);

    List<DocumentoEntity> findAllByPlanoIdAndClassificacaoOrderByNomeAsc(
            UUID planoId,
            Classificacao classificacao
    );

    Optional<DocumentoEntity> findByIdAndPlanoId(UUID id, UUID planoId);
}
