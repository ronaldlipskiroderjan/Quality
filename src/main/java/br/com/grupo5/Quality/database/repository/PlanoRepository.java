package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.PlanoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanoRepository extends JpaRepository<PlanoEntity, UUID> {
}
