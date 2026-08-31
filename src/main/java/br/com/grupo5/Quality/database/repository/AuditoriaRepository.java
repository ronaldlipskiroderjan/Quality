package br.com.grupo5.Quality.database.repository;

import br.com.grupo5.Quality.database.AuditoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, UUID> {
}
