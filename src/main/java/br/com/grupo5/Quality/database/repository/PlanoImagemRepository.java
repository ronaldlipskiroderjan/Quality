package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.PlanoImagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanoImagemRepository extends JpaRepository<PlanoImagemEntity, UUID> {
}
