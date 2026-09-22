package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.AtividadePlanoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AtividadePlanoRepository
        extends JpaRepository<AtividadePlanoEntity, UUID> {

    Page<AtividadePlanoEntity> findAllByPlanoId(UUID planoId, Pageable pageable);
}
